package itu.cloud.service;

import itu.cloud.dto.*;
import itu.cloud.entities.Utilisateur;
import itu.cloud.repositories.UtilisateurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Service d'authentification unifie.
 * Mode ONLINE (defaut): Firebase Auth + Firestore
 * Mode OFFLINE: PostgreSQL local
 * Les journaux sont toujours enregistres localement pour synchronisation ulterieure.
 */
@Service
public class AuthService {

    private final LocalAuthService localAuthService;
    private final FirebaseAuthService firebaseAuthService;
    private final FirestoreService firestoreService;
    private final JournalService journalService;
    private final ConnectivityService connectivityService;
    private final UtilisateurRepository utilisateurRepository;

    public AuthService(LocalAuthService localAuthService,
                       FirebaseAuthService firebaseAuthService,
                       FirestoreService firestoreService,
                       JournalService journalService,
                       ConnectivityService connectivityService,
                       UtilisateurRepository utilisateurRepository) {
        this.localAuthService = localAuthService;
        this.firebaseAuthService = firebaseAuthService;
        this.firestoreService = firestoreService;
        this.journalService = journalService;
        this.connectivityService = connectivityService;
        this.utilisateurRepository = utilisateurRepository;
    }

    /**
     * Inscription d'un utilisateur.
     * ONLINE: Cree dans Firebase Auth + Firestore, journalise localement
     * OFFLINE: Cree localement dans PostgreSQL, journalise pour sync ulterieure
     */
    public RegisterResponse register(RegisterRequest request) {
        // Validation
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

    /**
     * Inscription en mode ONLINE - Firebase par defaut
     */
    private RegisterResponse registerOnline(RegisterRequest request) {
        try {
            // 1. Creer l'utilisateur dans Firebase Auth
            LoginResponse firebaseAuth = firebaseAuthService.login(
                new LoginRequest() {{ setEmail(request.getEmail()); setPassword(request.getPassword()); }}
            );

            String firebaseUid = null;

            // Si l'utilisateur n'existe pas encore dans Firebase Auth, on doit le creer
            // Pour l'instant, on utilise l'API REST signUp
            if (!firebaseAuth.isSuccess()) {
                // Creer le compte Firebase
                firebaseUid = createFirebaseUser(request.getEmail(), request.getPassword());
                if (firebaseUid == null) {
                    // Fallback vers local si creation Firebase echoue
                    return registerOffline(request);
                }
            } else {
                firebaseUid = firebaseAuth.getData().getLocalId();
            }

            // 2. Sauvegarder dans Firestore
            Integer localId = generateLocalId();
            firestoreService.saveUtilisateur(
                localId,
                request.getEmail(),
                request.getNom(),
                firebaseUid,
                1,
                Instant.now()
            );

            // 3. Journaliser localement (pour audit et backup)
            journalService.logCreationUtilisateur(localId, request.getEmail(), request.getNom());

            return RegisterResponse.builder()
                    .success(true)
                    .data(RegisterResponse.UserData.builder()
                            .id(localId)
                            .email(request.getEmail())
                            .nom(request.getNom())
                            .role("USER")
                            .statut("ACTIF")
                            .build())
                    .build();

        } catch (Exception e) {
            // Fallback vers local en cas d'erreur
            System.err.println("Erreur mode online, fallback vers local: " + e.getMessage());
            return registerOffline(request);
        }
    }

    /**
     * Inscription en mode OFFLINE - PostgreSQL local
     */
    private RegisterResponse registerOffline(RegisterRequest request) {
        return localAuthService.register(request);
    }

    /**
     * Cree un utilisateur dans Firebase Auth via l'API REST signUp
     */
    private String createFirebaseUser(String email, String password) {
        try {
            return firebaseAuthService.createUser(email, password);
        } catch (Exception e) {
            System.err.println("Erreur creation utilisateur Firebase: " + e.getMessage());
            return null;
        }
    }

    /**
     * Genere un ID local unique
     */
    private Integer generateLocalId() {
        return (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
    }

    /**
     * Connexion d'un utilisateur.
     * ONLINE: Firebase Auth
     * OFFLINE: PostgreSQL local
     */
    public AuthResponse login(LoginRequest request) {
        // Validation
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

    /**
     * Connexion en mode ONLINE - Firebase Auth
     */
    private AuthResponse loginOnline(LoginRequest request) {
        try {
            LoginResponse firebaseResponse = firebaseAuthService.login(request);

            if (firebaseResponse.isSuccess()) {
                // Journaliser la connexion reussie
                journalService.logConnexionReussie(null, request.getEmail(), "ONLINE");

                return AuthResponse.builder()
                        .success(true)
                        .authMode("ONLINE")
                        .data(AuthResponse.AuthData.builder()
                                .email(firebaseResponse.getData().getEmail())
                                .nom(firebaseResponse.getData().getDisplayName())
                                .firebaseUid(firebaseResponse.getData().getLocalId())
                                .idToken(firebaseResponse.getData().getIdToken())
                                .refreshToken(firebaseResponse.getData().getRefreshToken())
                                .expiresIn(Long.parseLong(firebaseResponse.getData().getExpiresIn()))
                                .build())
                        .build();
            }

            // Echec Firebase - journaliser
            journalService.logConnexionEchouee(request.getEmail(), "FIREBASE_AUTH_FAILED");

            return AuthResponse.builder()
                    .success(false)
                    .error(firebaseResponse.getError())
                    .authMode("ONLINE")
                    .build();

        } catch (Exception e) {
            // Fallback vers local en cas d'erreur reseau
            System.err.println("Erreur mode online, fallback vers local: " + e.getMessage());
            return loginOffline(request);
        }
    }

    /**
     * Connexion en mode OFFLINE - PostgreSQL local
     */
    private AuthResponse loginOffline(LoginRequest request) {
        AuthResponse response = localAuthService.login(request);
        if (response != null) {
            response.setAuthMode("OFFLINE");
        }
        return response;
    }

    /**
     * Debloquer un utilisateur
     */
    @Transactional
    public boolean debloquerUtilisateur(String email) {
        boolean result = localAuthService.debloquerUtilisateur(email);

        // Si online, mettre a jour aussi dans Firestore
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

    /**
     * Retourne le mode actuel
     */
    public String getAuthMode() {
        return connectivityService.getEffectiveStatus();
    }

    /**
     * Retourne le mode configure
     */
    public String getConfiguredMode() {
        return connectivityService.getCurrentMode();
    }
}
