package itu.cloud.service;

import itu.cloud.collections.UtilisateurCollection;
import itu.cloud.dto.AuthDTO;
import itu.cloud.dto.LoginRequest;
import itu.cloud.dto.RegisterRequest;
import itu.cloud.entities.Utilisateur;
import itu.cloud.firebase.services.FirebaseService;
import itu.cloud.firebase.services.UtilisateurFirebaseService;
import itu.cloud.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
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
    private final JournalService journalService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(JwtUtil jwtUtil,
                       ParametreService parametreService,
                       FirebaseService firebaseService,
                       UtilisateurService utilisateurService,
                       UtilisateurFirebaseService utilisateurFirebaseService,
                       RoleService roleService,
                       JournalService journalService, PasswordEncoder passwordEncoder) {
        this.jwtUtil = jwtUtil;
        this.parametreService = parametreService;
        this.firebaseService = firebaseService;
        this.utilisateurService = utilisateurService;
        this.utilisateurFirebaseService = utilisateurFirebaseService;
        this.roleService = roleService;
        this.journalService = journalService;
        this.passwordEncoder = passwordEncoder;
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

            String encodedPassword = passwordEncoder.encode(data.getPassword());

            Map<String, String> userData = utilisateurService.registerWithDatabase(
                data.getEmail(),
                encodedPassword,
                data.getNom(),
                data.getRole()
            );

            UtilisateurCollection utilisateur = new UtilisateurCollection();
            utilisateur.setEmail(userData.get("email"));
            utilisateur.setNom(userData.get("displayName"));
            utilisateur.setId(Integer.valueOf(userData.get("postgres_id")));
            utilisateur.setRole(userData.get("role"));
            utilisateur.setActif(false);
            utilisateur.setTentativesEchouees(0);
            utilisateur.setVersion(1);
            utilisateur.setSynchronise(false);

            DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
            String now = LocalDateTime.now().format(formatter);
            utilisateur.setDateCreation(now);
            utilisateur.setDateMiseAJour(now);
            
            Map<String, Object> donneesAvecPassword = new HashMap<>();
            donneesAvecPassword.put("email", utilisateur.getEmail());
            donneesAvecPassword.put("nom", utilisateur.getNom());
            donneesAvecPassword.put("id", utilisateur.getId());
            donneesAvecPassword.put("role", utilisateur.getRole());
            donneesAvecPassword.put("actif", utilisateur.isActif());
            donneesAvecPassword.put("tentativesEchouees", utilisateur.getTentativesEchouees());
            donneesAvecPassword.put("version", utilisateur.getVersion());
            donneesAvecPassword.put("synchronise", utilisateur.isSynchronise());
            donneesAvecPassword.put("dateCreation", utilisateur.getDateCreation());
            donneesAvecPassword.put("dateMiseAJour", utilisateur.getDateMiseAJour());
            donneesAvecPassword.put("password", data.getPassword());

            journalService.journaliser(
                "UtilisateurCollection",
                "INSERT",
                null,
                donneesAvecPassword,
                1
            );

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

            Map<String, String> firebaseUserData = new HashMap<>();
            String userRole = null;

            Optional<Utilisateur> u = this.utilisateurService.findByEmail(loginRequest.getData().getEmail());
            if(u.isPresent()) {
                if(u.get().getFirebaseUid() == null) {
                    throw new RuntimeException("Le compte n'est pas encore activé");
                }
            } else {
                throw new RuntimeException("Aucun compte utilisateur n'appartient à cette adresse email");
            }

            boolean firebaseIsAvailable = false;
            try {
                firebaseIsAvailable = this.firebaseService.isAvailable();
            } catch(Exception e) {
                System.out.println("[Firebase unavailable]: "+e.getMessage());
            }

            System.out.println("[Firebase availability]: "+(firebaseIsAvailable ? " available" : " unavailable"));

            if (loginRequest.getIsOnline() != null && loginRequest.getIsOnline() && firebaseIsAvailable) {
                try {
                    utilisateurService.checkIfBlocked(data.getEmail());
                } catch (RuntimeException e) {
                    throw e;
                }

                try {
                    firebaseUserData = firebaseService.authenticateWithFirebase(
                        data.getEmail(),
                        data.getPassword()
                    );

                    utilisateurService.resetFailedAttemptsIfNeeded(data.getEmail());

                    try {
                        Optional<UtilisateurCollection> utilisateurOpt = utilisateurFirebaseService.findByEmail(data.getEmail());
                        if (utilisateurOpt.isPresent()) {
                            userRole = utilisateurOpt.get().getRole();
                        }
                    } catch (ExecutionException | InterruptedException e) {
                        throw new RuntimeException("Erreur lors de la récupération du role: " + e.getMessage());
                    }

                } catch (HttpClientErrorException e) {
                    try {
                        utilisateurService.handleFailedLoginByEmail(data.getEmail());
                    } catch (Exception ex) {
                        System.err.println("Erreur lors de l'incrémentation des tentatives: " + ex.getMessage());
                    }
                    throw new RuntimeException("Identifiants invalides: "+e.getMessage());
                } catch (RuntimeException e) {
                    if (e.getMessage() != null && e.getMessage().contains("authentification")) {
                        try {
                            utilisateurService.handleFailedLoginByEmail(data.getEmail());
                        } catch (Exception ex) {
                            System.err.println("Erreur lors de l'incrémentation des tentatives: " + ex.getMessage());
                        }
                    }
                   throw new RuntimeException("Identifiants invalides: "+e.getMessage());
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

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'authentification: " + e.getMessage());
        }
    }


}
