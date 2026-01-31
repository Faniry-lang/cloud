package itu.cloud.service;

import itu.cloud.dto.*;
import itu.cloud.entities.Utilisateur;
import itu.cloud.repositories.RolesUtilisateurRepository;
import itu.cloud.repositories.UtilisateurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
    private final ParametreService parametreService;
    private final itu.cloud.security.JwtUtil jwtUtil;

    public AuthService(LocalAuthService localAuthService,
                       FirebaseAuthService firebaseAuthService,
                       FirestoreService firestoreService,
                       JournalService journalService,
                       ConnectivityService connectivityService,
                       UtilisateurRepository utilisateurRepository,
                       RolesUtilisateurRepository rolesUtilisateurRepository,
                       ParametreService parametreService,
                       itu.cloud.security.JwtUtil jwtUtil) {
        this.localAuthService = localAuthService;
        this.firebaseAuthService = firebaseAuthService;
        this.firestoreService = firestoreService;
        this.journalService = journalService;
        this.connectivityService = connectivityService;
        this.utilisateurRepository = utilisateurRepository;
        this.rolesUtilisateurRepository = rolesUtilisateurRepository;
        this.parametreService = parametreService;
        this.jwtUtil = jwtUtil;
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


            Integer localId = generateLocalId();
            firestoreService.saveUtilisateur(
                localId,
                request.getEmail(),
                request.getNom(),
                firebaseUid,
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

    /**
     * Essaye d'authentifier via Firebase en priorité. En cas d'echec on incrémente les tentatives locales
     * et on applique le blocage si besoin. En cas d'exception on retombe en local.
     */
    private AuthResponse loginOnline(LoginRequest request) {
        try {
            // Vérifier localement si le compte est déjà bloqué (meilleur UX, évite d'appeler Firebase inutilement)
            Optional<Utilisateur> optLocalPre = utilisateurRepository.findByEmail(request.getEmail());
            if (optLocalPre.isPresent()) {
                Utilisateur uPre = optLocalPre.get();
                if (uPre.getBloqueJusqua() != null && Instant.now().isBefore(uPre.getBloqueJusqua())) {
                    journalService.logConnexionEchouee(request.getEmail(), "ACCOUNT_BLOCKED");
                    return AuthResponse.builder()
                            .success(false)
                            .error("Compte bloqué temporairement. Réessayez plus tard.")
                            .authMode("ONLINE")
                            .build();
                }
            }

            LoginResponse firebaseResponse = firebaseAuthService.login(request);

            if (firebaseResponse.isSuccess()) {
                // Si l'utilisateur existe localement, reset des tentatives et du blocage
                Optional<Utilisateur> optUser = utilisateurRepository.findByEmail(request.getEmail());
                if (optUser.isPresent()) {
                    Utilisateur utilisateur = optUser.get();
                    utilisateur.setTentativesEchouees(0);
                    utilisateur.setBloqueJusqua(null);
                    utilisateur.setDateMisAJour(Instant.now());
                    utilisateurRepository.save(utilisateur);

                    // Propager l'etat reset sur Firestore (best-effort)
                    try {
                        if (utilisateur.getId() != null && firestoreService.isAvailable()) {
                            firestoreService.saveUtilisateurState(utilisateur.getId(), 0, null, utilisateur.getActif(), utilisateur.getEmail());
                        }
                    } catch (Exception ex) {
                        System.err.println("Erreur propagation reset etat utilisateur vers Firestore: " + ex.getMessage());
                    }
                }

                // Récupérer le rôle local si présent
                String role = "VISITOR"; // Rôle par défaut
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
                                .token(generateJwt(optUser.orElse(null), role))
                                .idToken(firebaseResponse.getData().getIdToken())
                                .refreshToken(firebaseResponse.getData().getRefreshToken())
                                .expiresIn((long) parametreService.getJwtExpirationMinutes() * 60L)
                                .build())
                        .build();
            }

            // Firebase a répondu une erreur (mauvais mdp / email non trouvé etc.)
            // Journaliser
            journalService.logConnexionEchouee(request.getEmail(), "FIREBASE_AUTH_FAILED");

            // Incrémenter tentatives locales et bloquer si seuil atteint
            Optional<Utilisateur> optLocal = utilisateurRepository.findByEmail(request.getEmail());
            if (optLocal.isPresent()) {
                Utilisateur utilisateur = optLocal.get();
                int tentatives = (utilisateur.getTentativesEchouees() != null ? utilisateur.getTentativesEchouees() : 0) + 1;
                utilisateur.setTentativesEchouees(tentatives);
                utilisateur.setDateMisAJour(Instant.now());

                Instant bloqueJusqua = null;
                boolean nowBlocked = false;
                if (tentatives >= parametreService.getMaxFailedAttempts()) {
                    bloqueJusqua = Instant.now().plus(parametreService.getBlockDurationMinutes(), ChronoUnit.MINUTES);
                    utilisateur.setBloqueJusqua(bloqueJusqua);
                    journalService.logBlocageCompte(utilisateur.getId(), request.getEmail(), bloqueJusqua);
                    nowBlocked = true;
                }

                utilisateurRepository.save(utilisateur);

                // Best-effort: propager l'etat sur Firestore si possible
                try {
                    if (utilisateur.getId() != null && firestoreService.isAvailable()) {
                        firestoreService.saveUtilisateurState(utilisateur.getId(), utilisateur.getTentativesEchouees(), utilisateur.getBloqueJusqua(), utilisateur.getActif(), utilisateur.getEmail());
                    }
                } catch (Exception ex) {
                    System.err.println("Erreur propagation etat utilisateur vers Firestore: " + ex.getMessage());
                }

                if (nowBlocked) {
                    return AuthResponse.builder()
                            .success(false)
                            .error("Trop de tentatives. Compte bloqué temporairement.")
                            .authMode("ONLINE")
                            .build();
                }
            }

            // Aucun blocage atteint: retourner l'erreur fournie par Firebase (deja traduite)
            String err = firebaseResponse.getError();
            if (err == null || err.isBlank()) err = "Email ou mot de passe incorrect";

            return AuthResponse.builder()
                    .success(false)
                    .error(err)
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
            Utilisateur utilisateur = utilisateurRepository.findByEmail(request.getEmail()).orElse(null);
            String role = utilisateur != null ? getRoleUtilisateur(utilisateur) : "VISITOR";

            AuthResponse.AuthData authData = AuthResponse.AuthData.builder()
                    .userId(utilisateur != null ? utilisateur.getId() : null)
                    .email(utilisateur != null ? utilisateur.getEmail() : null)
                    .nom(utilisateur != null ? utilisateur.getNom() : null)
                    .role(role)
                    .token(utilisateur != null ? generateJwt(utilisateur, role) : null)
                    .build();

            response.setData(authData);
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

    /**
     * Met à jour les informations utilisateurs (online first then fallback local)
     */
    @Transactional
    public Map<String, Object> updateUserByEmail(String email, Map<String, Object> updates) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email requis");
        }

        if (connectivityService.isOnline()) {
            try {
                // Recherche par email dans Firestore (on obtient aussi firebase_id)
                Optional<Map<String, Object>> optRemote = firestoreService.getUtilisateurByEmail(email);
                if (optRemote.isPresent()) {
                    Map<String, Object> remote = optRemote.get();
                    String docId = remote.get("firebase_id") != null ? (String) remote.get("firebase_id") : null;

                    // apply updates locally if exists (lookup by email)
                    Optional<Utilisateur> optLocal = utilisateurRepository.findByEmail(email);
                    if (optLocal.isPresent()) {
                        Utilisateur u = optLocal.get();
                        if (updates.containsKey("nom")) u.setNom((String) updates.get("nom"));
                        if (updates.containsKey("actif")) u.setActif(Boolean.parseBoolean(String.valueOf(updates.get("actif"))));
                        u.setDateMisAJour(Instant.now());
                        utilisateurRepository.save(u);
                    }

                    // save to Firestore, prefer updating by docId if present
                    String role = updates.containsKey("role") ? (String) updates.get("role") : (String) remote.get("role");
                    String nom = updates.containsKey("nom") ? (String) updates.get("nom") : (String) remote.get("nom");
                    Integer version = null;
                    if (remote.get("version") instanceof Number) {
                        version = ((Number) remote.get("version")).intValue();
                    } else if (remote.get("version") instanceof String) {
                        try { version = Integer.parseInt((String) remote.get("version")); } catch (NumberFormatException ignored) {}
                    }
                    if (version == null) version = 1;

                    Integer localId = null;
                    if (optLocal.isPresent()) localId = optLocal.get().getId();
                    else if (remote.get("id") instanceof Number) localId = ((Number) remote.get("id")).intValue();

                    String firebaseUid = (String) remote.get("firebaseUid");

                    if (docId != null) {
                        if (role != null) {
                            firestoreService.saveUtilisateurWithRoleByDocId(docId, localId, email, nom, firebaseUid, role, version, Instant.now());
                        } else {
                            firestoreService.saveUtilisateurByDocId(docId, localId, email, nom, firebaseUid, version, Instant.now());
                        }
                    } else {
                        // fallback: update by numeric id stored in remote (less preferred)
                        Integer remoteId = remote.get("id") instanceof Number ? ((Number) remote.get("id")).intValue() : null;
                        if (remoteId != null) {
                            if (role != null) {
                                firestoreService.saveUtilisateurWithRole(remoteId, email, nom, firebaseUid, role, version, Instant.now());
                            } else {
                                firestoreService.saveUtilisateur(remoteId, email, nom, firebaseUid, version, Instant.now());
                            }
                        }
                    }

                    return Map.of("success", true, "message", "Utilisateur mis a jour en ligne");
                }
            } catch (Exception e) {
                System.err.println("Erreur mise a jour en ligne, fallback local: " + e.getMessage());
                // proceed to local
            }
        }

        // local fallback: lookup by email
        Optional<Utilisateur> optLocal = utilisateurRepository.findByEmail(email);
        if (optLocal.isPresent()) {
            Utilisateur u = optLocal.get();
            if (updates.containsKey("nom")) u.setNom((String) updates.get("nom"));
            if (updates.containsKey("actif")) u.setActif(Boolean.parseBoolean(String.valueOf(updates.get("actif"))));
            u.setDateMisAJour(Instant.now());
            utilisateurRepository.save(u);
            return Map.of("success", true, "message", "Utilisateur mis a jour localement");
        }

        return Map.of("success", false, "error", "Utilisateur non trouve");
    }

    private String generateJwt(Utilisateur utilisateur, String role) {
        if (utilisateur == null) return null;
        try {
            String secret = parametreService.getJwtSecret();
            int expMinutes = parametreService.getJwtExpirationMinutes();
            java.util.Map<String, Object> claims = new java.util.HashMap<>();
            claims.put("userId", utilisateur.getId());
            claims.put("email", utilisateur.getEmail());
            claims.put("role", role != null ? role : "USER");
            if (utilisateur.getFirebaseUid() != null) claims.put("firebaseUid", utilisateur.getFirebaseUid());
            return jwtUtil.generateToken(secret, claims, expMinutes);
        } catch (Exception e) {
            System.err.println("Erreur generation JWT: " + e.getMessage());
            return null;
        }
    }

}
