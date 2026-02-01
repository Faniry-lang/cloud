package itu.cloud.service;

import itu.cloud.collections.UtilisateurCollection;
import itu.cloud.dto.AuthDTO;
import itu.cloud.dto.LoginRequest;
import itu.cloud.dto.RegisterRequest;
import itu.cloud.firebase.services.FirebaseService;
import itu.cloud.firebase.services.UtilisateurFirebaseService;
import itu.cloud.security.JwtUtil;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Service
public class AuthService {

    private final JwtUtil jwtUtil;
    private final ParametreService parametreService;
    private final FirebaseService firebaseService;
    private final UtilisateurService utilisateurService;
    private final UtilisateurFirebaseService utilisateurFirebaseService;
    private final RoleService roleService;

    public AuthService(JwtUtil jwtUtil,
                      ParametreService parametreService,
                      FirebaseService firebaseService,
                      UtilisateurService utilisateurService,
                      UtilisateurFirebaseService utilisateurFirebaseService,
                      RoleService roleService) {
        this.jwtUtil = jwtUtil;
        this.parametreService = parametreService;
        this.firebaseService = firebaseService;
        this.utilisateurService = utilisateurService;
        this.utilisateurFirebaseService = utilisateurFirebaseService;
        this.roleService = roleService;
    }

    public UtilisateurCollection register(RegisterRequest registerRequest) {
        try {
            if (registerRequest.getData() == null) {
                throw new RuntimeException("Les données d'inscription sont requises");
            }

            RegisterRequest.RegisterData data = registerRequest.getData();

            if (data.getRole() == null || data.getRole().isEmpty()) {
                throw new RuntimeException("Le role est requis");
            }

            if (!roleService.isValidRole(data.getRole())) {
                throw new RuntimeException("Role invalide");
            }

            Map<String, String> userData;
            UtilisateurCollection utilisateur = new UtilisateurCollection();

            if (registerRequest.getIsOnline() != null && registerRequest.getIsOnline()) {
                userData = firebaseService.registerWithFirebase(
                    data.getEmail(),
                    data.getPassword(),
                    data.getNom()
                );

                utilisateur.setEmail(userData.get("email"));
                utilisateur.setNom(userData.get("displayName"));
                utilisateur.setFirebaseUid(userData.get("localId"));
                utilisateur.setRole(data.getRole());
                utilisateur.setActif(true);
                utilisateur.setTentativesEchouees(0);
                utilisateur.setVersion(1);
                utilisateur.setSynchronise(true);

                DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
                String now = LocalDateTime.now().format(formatter);
                utilisateur.setDateCreation(now);
                utilisateur.setDateMiseAJour(now);

                try {
                    utilisateurFirebaseService.save(utilisateur);
                } catch (ExecutionException | InterruptedException e) {
                    throw new RuntimeException("Erreur lors de la sauvegarde dans Firestore: " + e.getMessage(), e);
                }

            } else {
                userData = utilisateurService.registerWithDatabase(
                    data.getEmail(),
                    data.getPassword(),
                    data.getNom(),
                    data.getRole()
                );

                utilisateur.setEmail(userData.get("email"));
                utilisateur.setNom(userData.get("displayName"));
                utilisateur.setFirebaseUid(userData.get("localId"));
                utilisateur.setRole(userData.get("role"));
                utilisateur.setActif(true);
                utilisateur.setTentativesEchouees(0);
                utilisateur.setVersion(1);
                utilisateur.setSynchronise(false);

                DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
                String now = LocalDateTime.now().format(formatter);
                utilisateur.setDateCreation(now);
                utilisateur.setDateMiseAJour(now);
            }

            return utilisateur;

        } catch (HttpClientErrorException e) {
            throw new RuntimeException("Erreur lors de l'inscription: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'inscription: " + e.getMessage());
        }
    }

    public AuthDTO login(LoginRequest loginRequest) {
        try {
            if (loginRequest.getData() == null) {
                throw new RuntimeException("Les données de connexion sont requises");
            }

            LoginRequest.LoginData data = loginRequest.getData();

            Map<String, String> firebaseUserData;
            String userRole = null;

            if (loginRequest.getIsOnline() != null && loginRequest.getIsOnline()) {
                firebaseUserData = firebaseService.authenticateWithFirebase(
                    data.getEmail(),
                    data.getPassword()
                );

                try {
                    Optional<UtilisateurCollection> utilisateurOpt = utilisateurFirebaseService.findByEmail(data.getEmail());
                    if (utilisateurOpt.isPresent()) {
                        userRole = utilisateurOpt.get().getRole();
                        System.out.println("USER ROLE");
                        System.out.println(userRole);
                    }
                } catch (ExecutionException | InterruptedException e) {
                    throw new RuntimeException("Erreur lors de la récupération du role: " + e.getMessage());
                }

            } else {
                firebaseUserData = utilisateurService.authenticateWithDatabase(
                    data.getEmail(),
                    data.getPassword()
                );
                userRole = firebaseUserData.get("role");
            }


            if (userRole == null || !roleService.isManagerRole(userRole)) {
                throw new RuntimeException("Vous n'êtes pas autorisé");
            }

            Integer sessionDurationMinutes = parametreService.getValeurAsInteger("SESSION_DURATION_MINUTES");
            if (sessionDurationMinutes == null) {
                sessionDurationMinutes = 60;
            }

            String token = jwtUtil.generateToken(firebaseUserData.get("email"), sessionDurationMinutes);

            return AuthDTO.builder()
                    .nom(firebaseUserData.get("displayName"))
                    .email(firebaseUserData.get("email"))
                    .token(token)
                    .build();

        } catch (HttpClientErrorException e) {
            throw new RuntimeException("Identifiants invalides");
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'authentification: " + e.getMessage());
        }
    }


}
