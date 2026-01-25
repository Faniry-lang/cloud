package itu.cloud.service;

import itu.cloud.dto.*;
import itu.cloud.entities.Utilisateur;
import itu.cloud.repositories.RolesUtilisateurRepository;
import itu.cloud.repositories.UtilisateurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthService {

    private final LocalAuthService localAuthService;
    private final FirebaseAuthService firebaseAuthService;
    private final FirestoreService firestoreService;
    private final JournalService journalService;
    private final ConnectivityService connectivityService;
    private final UtilisateurRepository utilisateurRepository;
    private final RolesUtilisateurRepository rolesUtilisateurRepository;

    public AuthService(LocalAuthService localAuthService,
                       FirebaseAuthService firebaseAuthService,
                       FirestoreService firestoreService,
                       JournalService journalService,
                       ConnectivityService connectivityService,
                       UtilisateurRepository utilisateurRepository,
                       RolesUtilisateurRepository rolesUtilisateurRepository) {
        this.localAuthService = localAuthService;
        this.firebaseAuthService = firebaseAuthService;
        this.firestoreService = firestoreService;
        this.journalService = journalService;
        this.connectivityService = connectivityService;
        this.utilisateurRepository = utilisateurRepository;
        this.rolesUtilisateurRepository = rolesUtilisateurRepository;
    }

    public RegisterResponse register(RegisterRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            return RegisterResponse.builder().success(false).error("Email requis").build();
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            return RegisterResponse.builder().success(false).error("Mot de passe requis (min 6 caracteres)").build();
        }

        if (connectivityService.isOnline()) {
            return registerOnline(request);
        } else {
            return registerOffline(request);
        }
    }

    private RegisterResponse registerOnline(RegisterRequest request) {
        try {
            LoginResponse firebaseAuth = firebaseAuthService.login(
                new LoginRequest() {{ setEmail(request.getEmail()); setPassword(request.getPassword()); }}
            );

            String firebaseUid = null;

            if (!firebaseAuth.isSuccess()) {
                firebaseUid = createFirebaseUser(request.getEmail(), request.getPassword());
                if (firebaseUid == null) {
                    return registerOffline(request);
                }
            } else {
                firebaseUid = firebaseAuth.getData().getLocalId();
            }

            // Normaliser le rôle (VISITOR par défaut)
            String roleNom = "VISITOR";
            if (request.getRole() != null && !request.getRole().isBlank()) {
                String roleUpper = request.getRole().toUpperCase();
                if (roleUpper.equals("VISITOR") || roleUpper.equals("MANAGER")) {
                    roleNom = roleUpper;
                }
            }

            Integer localId = generateLocalId();
            firestoreService.saveUtilisateurWithRole(
                localId,
                request.getEmail(),
                request.getNom(),
                firebaseUid,
                roleNom,
                1,
                Instant.now()
            );

            journalService.logCreationUtilisateur(localId, request.getEmail(), request.getNom());

            return RegisterResponse.builder()
                    .success(true)
                    .data(RegisterResponse.UserData.builder()
                            .id(localId)
                            .email(request.getEmail())
                            .nom(request.getNom())
                            .role(roleNom)
                            .statut("ACTIF")
                            .build())
                    .build();

        } catch (Exception e) {
            System.err.println("Erreur mode online, fallback vers local: " + e.getMessage());
            return registerOffline(request);
        }
    }

    private RegisterResponse registerOffline(RegisterRequest request) {
        return localAuthService.register(request);
    }


    private String createFirebaseUser(String email, String password) {
        try {
            return firebaseAuthService.createUser(email, password);
        } catch (Exception e) {
            System.err.println("Erreur creation utilisateur Firebase: " + e.getMessage());
            return null;
        }
    }

    private Integer generateLocalId() {
        return (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
    }

    public AuthResponse login(LoginRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank() ||
            request.getPassword() == null || request.getPassword().isBlank()) {
            return AuthResponse.builder()
                    .success(false)
                    .error("Email et mot de passe requis")
                    .build();
        }

        if (connectivityService.isOnline()) {
            return loginOnline(request);
        } else {
            return loginOffline(request);
        }
    }

    private AuthResponse loginOnline(LoginRequest request) {
        try {
            LoginResponse firebaseResponse = firebaseAuthService.login(request);

            if (firebaseResponse.isSuccess()) {
                // Récupérer l'utilisateur local pour obtenir son rôle
                String role = "VISITOR"; // Rôle par défaut
                Optional<Utilisateur> optUser = utilisateurRepository.findByEmail(request.getEmail());
                if (optUser.isPresent()) {
                    role = getRoleUtilisateur(optUser.get());
                }

                journalService.logConnexionReussie(
                    optUser.map(Utilisateur::getId).orElse(null),
                    request.getEmail(),
                    "ONLINE"
                );

                return AuthResponse.builder()
                        .success(true)
                        .authMode("ONLINE")
                        .data(AuthResponse.AuthData.builder()
                                .userId(optUser.map(Utilisateur::getId).orElse(null))
                                .email(firebaseResponse.getData().getEmail())
                                .nom(firebaseResponse.getData().getDisplayName())
                                .role(role)
                                .firebaseUid(firebaseResponse.getData().getLocalId())
                                .idToken(firebaseResponse.getData().getIdToken())
                                .refreshToken(firebaseResponse.getData().getRefreshToken())
                                .expiresIn(Long.parseLong(firebaseResponse.getData().getExpiresIn()))
                                .build())
                        .build();
            }

            journalService.logConnexionEchouee(request.getEmail(), "FIREBASE_AUTH_FAILED");

            return AuthResponse.builder()
                    .success(false)
                    .error(firebaseResponse.getError())
                    .authMode("ONLINE")
                    .build();

        } catch (Exception e) {
            System.err.println("Erreur mode online, fallback vers local: " + e.getMessage());
            return loginOffline(request);
        }
    }

    private AuthResponse loginOffline(LoginRequest request) {
        AuthResponse response = localAuthService.login(request);
        if (response != null) {
            response.setAuthMode("OFFLINE");
        }
        return response;
    }

    @Transactional
    public boolean debloquerUtilisateur(String email) {
        boolean result = localAuthService.debloquerUtilisateur(email);

        if (connectivityService.isOnline() && result) {
            try {
                Optional<Utilisateur> user = utilisateurRepository.findByEmail(email);
                if (user.isPresent()) {
                    firestoreService.saveUtilisateur(
                        user.get().getId(),
                        user.get().getEmail(),
                        user.get().getNom(),
                        user.get().getFirebaseUid(),
                        user.get().getVersion(),
                        user.get().getDateCreation()
                    );
                }
            } catch (Exception e) {
                System.err.println("Erreur sync Firestore apres deblocage: " + e.getMessage());
            }
        }

        return result;
    }


    public String getAuthMode() {
        return connectivityService.getEffectiveStatus();
    }

    public String getConfiguredMode() {
        return connectivityService.getCurrentMode();
    }

    private String getRoleUtilisateur(Utilisateur utilisateur) {
        return rolesUtilisateurRepository.findByIdUtilisateurAndDateSuppressionIsNull(utilisateur)
                .stream()
                .map(ru -> ru.getIdRole().getNom())
                .findFirst()
                .orElse("VISITOR");
    }


    public Map<String, Object> getStatutBlocage(String email) {
        Optional<Utilisateur> optUser = utilisateurRepository.findByEmail(email);

        if (optUser.isEmpty()) {
            throw new RuntimeException("Utilisateur non trouvé");
        }

        Utilisateur user = optUser.get();
        Map<String, Object> statut = new HashMap<>();
        statut.put("success", true);
        statut.put("email", email);
        statut.put("tentativesEchouees", user.getTentativesEchouees() != null ? user.getTentativesEchouees() : 0);

        boolean estBloque = user.getBloqueJusqua() != null && Instant.now().isBefore(user.getBloqueJusqua());
        statut.put("estBloque", estBloque);

        if (estBloque) {
            statut.put("bloqueJusqua", user.getBloqueJusqua().toString());
        } else {
            statut.put("bloqueJusqua", null);
        }

        return statut;
    }
}
