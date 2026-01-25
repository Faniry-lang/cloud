package itu.cloud.service;

import itu.cloud.dto.AuthResponse;
import itu.cloud.dto.LoginRequest;
import itu.cloud.dto.RegisterRequest;
import itu.cloud.dto.RegisterResponse;
import itu.cloud.entities.*;
import itu.cloud.repositories.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Service d'authentification locale.
 * Gere l'inscription, la connexion, et le blocage des comptes.
 * En mode hybride, synchronise automatiquement avec Firestore.
 */
@Service
public class LocalAuthService {

    private final UtilisateurRepository utilisateurRepository;
    private final RoleRepository roleRepository;
    private final StatutRepository statutRepository;
    private final RolesUtilisateurRepository rolesUtilisateurRepository;
    private final StatutsUtilisateurRepository statutsUtilisateurRepository;
    private final JournalService journalService;
    private final ParametreService parametreService;
    private final FirestoreService firestoreService;
    private final PasswordEncoder passwordEncoder;

    public LocalAuthService(UtilisateurRepository utilisateurRepository,
                           RoleRepository roleRepository,
                           StatutRepository statutRepository,
                           RolesUtilisateurRepository rolesUtilisateurRepository,
                           StatutsUtilisateurRepository statutsUtilisateurRepository,
                           JournalService journalService,
                           ParametreService parametreService,
                           FirestoreService firestoreService,
                           PasswordEncoder passwordEncoder) {
        this.utilisateurRepository = utilisateurRepository;
        this.roleRepository = roleRepository;
        this.statutRepository = statutRepository;
        this.rolesUtilisateurRepository = rolesUtilisateurRepository;
        this.statutsUtilisateurRepository = statutsUtilisateurRepository;
        this.journalService = journalService;
        this.parametreService = parametreService;
        this.firestoreService = firestoreService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Inscription d'un nouvel utilisateur (locale)
     */
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        // Validation des donnees
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            return RegisterResponse.builder()
                    .success(false)
                    .error("Email requis")
                    .build();
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            return RegisterResponse.builder()
                    .success(false)
                    .error("Mot de passe requis")
                    .build();
        }
        if (request.getPassword().length() < 6) {
            return RegisterResponse.builder()
                    .success(false)
                    .error("Le mot de passe doit contenir au moins 6 caracteres")
                    .build();
        }

        // Verifier unicite email
        if (utilisateurRepository.existsByEmail(request.getEmail())) {
            return RegisterResponse.builder()
                    .success(false)
                    .error("Un compte existe deja avec cet email")
                    .build();
        }

        // Creer l'utilisateur
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setEmail(request.getEmail());
        utilisateur.setNom(request.getNom());
        utilisateur.setMotDePasseHash(passwordEncoder.encode(request.getPassword()));
        utilisateur.setTentativesEchouees(0);
        utilisateur.setActif(true);
        utilisateur.setVersion(1);
        utilisateur.setDateCreation(Instant.now());

        utilisateur = utilisateurRepository.save(utilisateur);

        // Attribuer le role (celui de la requete ou VISITOR par defaut)
        String roleNom = assignerRole(utilisateur, request.getRole());

        // Attribuer le statut par defaut
        String statutDescription = assignerStatutParDefaut(utilisateur);

        // Journaliser la creation
        journalService.logCreationUtilisateur(utilisateur.getId(), utilisateur.getEmail(), utilisateur.getNom());
        journalService.logAttributionRole(utilisateur.getId(), roleNom);
        journalService.logAttributionStatut(utilisateur.getId(), statutDescription);

        // Synchroniser avec Firestore (best effort - ne bloque pas si echec)
        syncToFirestore(utilisateur);

        return RegisterResponse.builder()
                .success(true)
                .data(RegisterResponse.UserData.builder()
                        .id(utilisateur.getId())
                        .email(utilisateur.getEmail())
                        .nom(utilisateur.getNom())
                        .role(roleNom)
                        .statut(statutDescription)
                        .build())
                .build();
    }

    /**
     * Synchronise un utilisateur vers Firestore (best effort)
     */
    private void syncToFirestore(Utilisateur utilisateur) {
        try {
            if (firestoreService.isAvailable()) {
                firestoreService.saveUtilisateur(
                    utilisateur.getId(),
                    utilisateur.getEmail(),
                    utilisateur.getNom(),
                    utilisateur.getFirebaseUid(),
                    utilisateur.getVersion(),
                    utilisateur.getDateCreation()
                );
            }
        } catch (Exception e) {
            // Log l'erreur mais ne bloque pas l'inscription
            System.err.println("Erreur sync Firestore (non bloquante): " + e.getMessage());
        }
    }

    /**
     * Connexion locale d'un utilisateur
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        // Validation
        if (request.getEmail() == null || request.getEmail().isBlank() ||
            request.getPassword() == null || request.getPassword().isBlank()) {
            return AuthResponse.builder()
                    .success(false)
                    .error("Email et mot de passe requis")
                    .authMode("LOCAL")
                    .build();
        }

        // Charger l'utilisateur
        Optional<Utilisateur> optUtilisateur = utilisateurRepository.findByEmail(request.getEmail());
        if (optUtilisateur.isEmpty()) {
            journalService.logConnexionEchouee(request.getEmail(), "EMAIL_NOT_FOUND");
            return AuthResponse.builder()
                    .success(false)
                    .error("Email ou mot de passe incorrect")
                    .authMode("LOCAL")
                    .build();
        }

        Utilisateur utilisateur = optUtilisateur.get();

        // Vérifier si actif
        if (utilisateur.getActif() == null || !utilisateur.getActif()) {
            journalService.logConnexionEchouee(request.getEmail(), "ACCOUNT_DISABLED");
            return AuthResponse.builder()
                    .success(false)
                    .error("Ce compte a été désactivé")
                    .authMode("LOCAL")
                    .build();
        }

        // Vérifier le blocage
        if (utilisateur.getBloqueJusqua() != null && Instant.now().isBefore(utilisateur.getBloqueJusqua())) {
            journalService.logConnexionEchouee(request.getEmail(), "ACCOUNT_BLOCKED");
            return AuthResponse.builder()
                    .success(false)
                    .error("Compte bloqué temporairement. Réessayez plus tard.")
                    .authMode("LOCAL")
                    .build();
        }

        // Vérifier le mot de passe
        if (utilisateur.getMotDePasseHash() == null ||
            !passwordEncoder.matches(request.getPassword(), utilisateur.getMotDePasseHash())) {
            return gererEchecConnexion(utilisateur, request.getEmail());
        }

        // Succès - réinitialiser les tentatives
        utilisateur.setTentativesEchouees(0);
        utilisateur.setBloqueJusqua(null);
        utilisateur.setDateMisAJour(Instant.now());
        utilisateurRepository.save(utilisateur);

        // Journaliser le succès
        journalService.logConnexionReussie(utilisateur.getId(), utilisateur.getEmail(), "LOCAL");

        // Récupérer rôle et statut
        String role = getRoleUtilisateur(utilisateur);
        String statut = getStatutUtilisateur(utilisateur);

        // TODO V2: Si online, déléguer l'authentification à Firebase Auth

        return AuthResponse.builder()
                .success(true)
                .authMode("LOCAL")
                .data(AuthResponse.AuthData.builder()
                        .userId(utilisateur.getId())
                        .email(utilisateur.getEmail())
                        .nom(utilisateur.getNom())
                        .role(role)
                        .statut(statut)
                        // Token JWT sera ajouté plus tard si nécessaire
                        .build())
                .build();
    }

    /**
     * Débloquer un utilisateur (admin)
     */
    @Transactional
    public boolean debloquerUtilisateur(String email) {
        Optional<Utilisateur> optUtilisateur = utilisateurRepository.findByEmail(email);
        if (optUtilisateur.isEmpty()) {
            return false;
        }

        Utilisateur utilisateur = optUtilisateur.get();
        utilisateur.setTentativesEchouees(0);
        utilisateur.setBloqueJusqua(null);
        utilisateur.setDateMisAJour(Instant.now());
        utilisateurRepository.save(utilisateur);

        journalService.logDeblocageCompte(utilisateur.getId(), utilisateur.getEmail());
        return true;
    }

    /**
     * Gère l'échec de connexion (incrément tentatives, blocage si seuil)
     */
    private AuthResponse gererEchecConnexion(Utilisateur utilisateur, String email) {
        int tentatives = (utilisateur.getTentativesEchouees() != null ? utilisateur.getTentativesEchouees() : 0) + 1;
        utilisateur.setTentativesEchouees(tentatives);
        utilisateur.setDateMisAJour(Instant.now());

        String errorMessage = "Email ou mot de passe incorrect";

        if (tentatives >= parametreService.getMaxFailedAttempts()) {
            Instant bloqueJusqua = Instant.now().plus(parametreService.getBlockDurationMinutes(), ChronoUnit.MINUTES);
            utilisateur.setBloqueJusqua(bloqueJusqua);
            errorMessage = "Trop de tentatives. Compte bloqué temporairement.";
            journalService.logBlocageCompte(utilisateur.getId(), email, bloqueJusqua);
        }

        utilisateurRepository.save(utilisateur);
        journalService.logConnexionEchouee(email, "INVALID_PASSWORD");

        return AuthResponse.builder()
                .success(false)
                .error(errorMessage)
                .authMode("LOCAL")
                .build();
    }

    /**
     * Attribue un rôle à l'utilisateur (VISITOR par défaut si non spécifié)
     */
    private String assignerRole(Utilisateur utilisateur, String roleRequest) {
        // Normaliser le nom du rôle (uppercase)
        String roleNom;
        if (roleRequest != null && !roleRequest.isBlank()) {
            roleNom = roleRequest.toUpperCase();
            // Vérifier que c'est un rôle valide (VISITOR ou MANAGER)
            if (!roleNom.equals("VISITOR") && !roleNom.equals("MANAGER")) {
                roleNom = "VISITOR"; // Fallback si rôle invalide
            }
        } else {
            roleNom = parametreService.getDefaultRole(); // VISITOR par défaut
        }

        Optional<Role> optRole = roleRepository.findByNom(roleNom);

        Role role;
        if (optRole.isEmpty()) {
            // Créer le rôle s'il n'existe pas
            role = new Role();
            role.setNom(roleNom);
            role.setDateCreation(Instant.now());
            role = roleRepository.save(role);
        } else {
            role = optRole.get();
        }

        RolesUtilisateurId id = new RolesUtilisateurId();
        id.setIdUtilisateur(utilisateur.getId());
        id.setIdRole(role.getId());

        RolesUtilisateur rolesUtilisateur = new RolesUtilisateur();
        rolesUtilisateur.setId(id);
        rolesUtilisateur.setIdUtilisateur(utilisateur);
        rolesUtilisateur.setIdRole(role);
        rolesUtilisateur.setDateCreation(Instant.now());

        rolesUtilisateurRepository.save(rolesUtilisateur);
        return role.getNom();
    }

    /**
     * Attribue le statut par défaut à l'utilisateur
     */
    private String assignerStatutParDefaut(Utilisateur utilisateur) {
        String defaultStatutDesc = parametreService.getDefaultStatus();
        Optional<Statut> optStatut = statutRepository.findByDescription(defaultStatutDesc);

        Statut statut;
        if (optStatut.isEmpty()) {
            // Créer le statut s'il n'existe pas
            statut = new Statut();
            statut.setDescription(defaultStatutDesc);
            statut.setDateCreation(Instant.now());
            statut = statutRepository.save(statut);
        } else {
            statut = optStatut.get();
        }

        StatutsUtilisateurId id = new StatutsUtilisateurId();
        id.setIdUtilisateur(utilisateur.getId());
        id.setIdStatut(statut.getId());

        StatutsUtilisateur statutsUtilisateur = new StatutsUtilisateur();
        statutsUtilisateur.setId(id);
        statutsUtilisateur.setIdUtilisateur(utilisateur);
        statutsUtilisateur.setIdStatut(statut);
        statutsUtilisateur.setDateCreation(Instant.now());

        statutsUtilisateurRepository.save(statutsUtilisateur);
        return statut.getDescription();
    }

    /**
     * Récupère le rôle principal de l'utilisateur
     */
    private String getRoleUtilisateur(Utilisateur utilisateur) {
        return rolesUtilisateurRepository.findAll().stream()
                .filter(ru -> ru.getIdUtilisateur().getId().equals(utilisateur.getId()))
                .filter(ru -> ru.getDateSuppression() == null)
                .map(ru -> ru.getIdRole().getNom())
                .findFirst()
                .orElse("UNKNOWN");
    }

    /**
     * Récupère le statut actuel de l'utilisateur
     */
    private String getStatutUtilisateur(Utilisateur utilisateur) {
        return statutsUtilisateurRepository.findAll().stream()
                .filter(su -> su.getIdUtilisateur().getId().equals(utilisateur.getId()))
                .filter(su -> su.getDateSuppression() == null)
                .map(su -> su.getIdStatut().getDescription())
                .findFirst()
                .orElse("UNKNOWN");
    }

    /**
     * Vérifie si un utilisateur existe par email
     */
    public boolean existsByEmail(String email) {
        return utilisateurRepository.existsByEmail(email);
    }

    /**
     * Récupère un utilisateur par email
     */
    public Optional<Utilisateur> findByEmail(String email) {
        return utilisateurRepository.findByEmail(email);
    }
}

