package itu.cloud.service;

import itu.cloud.collections.UtilisateurCollection;
import itu.cloud.entities.Role;
import itu.cloud.entities.RoleUtilisateur;
import itu.cloud.entities.Utilisateur;
import itu.cloud.firebase.services.UtilisateurFirebaseService;
import itu.cloud.repositories.RoleUtilisateurRepository;
import itu.cloud.repositories.UtilisateurRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final ParametreService parametreService;
    private final PasswordEncoder passwordEncoder;
    private final RoleService roleService;
    private final RoleUtilisateurRepository roleUtilisateurRepository;
    private final UtilisateurFirebaseService utilisateurFirebaseService;
    private final JournalService journalService;

    public UtilisateurService(UtilisateurRepository utilisateurRepository,
                              ParametreService parametreService,
                              PasswordEncoder passwordEncoder,
                              RoleService roleService,
                              RoleUtilisateurRepository roleUtilisateurRepository,
                              UtilisateurFirebaseService utilisateurFirebaseService,
                              JournalService journalService) {
        this.utilisateurRepository = utilisateurRepository;
        this.parametreService = parametreService;
        this.passwordEncoder = passwordEncoder;
        this.roleService = roleService;
        this.roleUtilisateurRepository = roleUtilisateurRepository;
        this.utilisateurFirebaseService = utilisateurFirebaseService;
        this.journalService = journalService;
    }

    public Map<String, String> registerWithDatabase(String email, String password, String nom, String roleName) {
        if (utilisateurRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Un utilisateur avec cet email existe déjà");
        }

        Role role = roleService.findOrCreateRole(roleName);

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setEmail(email);
        utilisateur.setNom(nom != null ? nom : email);
        utilisateur.setMotDePasseHash(passwordEncoder.encode(password));
        utilisateur.setActif(true);
        utilisateur.setTentativesEchouees(0);
        utilisateur.setVersion(1);
        utilisateur.setDateCreation(LocalDateTime.now());

        Utilisateur savedUser = utilisateurRepository.save(utilisateur);

        RoleUtilisateur roleUtilisateur = new RoleUtilisateur();
        roleUtilisateur.setIdUtilisateur(savedUser);
        roleUtilisateur.setIdRole(role);
        roleUtilisateur.setDateCreation(LocalDateTime.now());
        roleUtilisateurRepository.save(roleUtilisateur);

        Map<String, String> userData = new HashMap<>();
        userData.put("email", savedUser.getEmail());
        userData.put("displayName", savedUser.getNom());
        userData.put("postgres_id", String.valueOf(savedUser.getId()));
        userData.put("role", roleName);

        return userData;
    }

    public Map<String, String> authenticateWithDatabase(String email, String password) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (utilisateur.getFirebaseUid() == null) {
            throw new RuntimeException("Le compte n'est pas encore activé");
        }

        if (utilisateur.getBloqueJusqua() != null &&
            utilisateur.getBloqueJusqua().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("Compte bloqué jusqu'à " + utilisateur.getBloqueJusqua());
        }

        if (utilisateur.getActif() == null || !utilisateur.getActif()) {
            throw new RuntimeException("Compte inactif");
        }

        if (utilisateur.getMotDePasseHash() == null ||
            !passwordEncoder.matches(password, utilisateur.getMotDePasseHash())) {
            handleFailedLogin(utilisateur);
            throw new RuntimeException("Identifiants invalides");
        }

        resetFailedAttemptsIfNeeded(email);

        List<RoleUtilisateur> rolesUtilisateur = roleUtilisateurRepository.findByIdUtilisateur(utilisateur);
        String roleName = null;
        if (!rolesUtilisateur.isEmpty()) {
            roleName = rolesUtilisateur.get(0).getIdRole().getNom();
        }

        Map<String, String> userData = new HashMap<>();
        userData.put("email", utilisateur.getEmail());
        userData.put("displayName", utilisateur.getNom() != null ? utilisateur.getNom() : utilisateur.getEmail());
        userData.put("localId", utilisateur.getFirebaseUid() != null ? utilisateur.getFirebaseUid() : String.valueOf(utilisateur.getId()));
        userData.put("role", roleName);

        return userData;
    }

    public void checkIfBlocked(String email) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (utilisateur.getBloqueJusqua() != null &&
            utilisateur.getBloqueJusqua().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("Compte bloqué jusqu'à " + utilisateur.getBloqueJusqua());
        }

        if (utilisateur.getActif() == null || !utilisateur.getActif()) {
            throw new RuntimeException("Compte inactif");
        }
    }

    public void handleFailedLoginByEmail(String email) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        handleFailedLogin(utilisateur);
    }

    public void resetFailedAttemptsIfNeeded(String email) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email).orElse(null);
        if (utilisateur != null && utilisateur.getTentativesEchouees() != null && utilisateur.getTentativesEchouees() > 0) {
            utilisateur.setTentativesEchouees(0);
            utilisateur.setBloqueJusqua(null);
            utilisateur.setDateMisAJour(LocalDateTime.now());
            Utilisateur savedUser = utilisateurRepository.save(utilisateur);

            UtilisateurCollection utilisateurCollection = convertToCollection(savedUser);

            boolean firestoreAvailable = false;
            try {
                Optional<UtilisateurCollection> firestoreUser = utilisateurFirebaseService.findByEmail(utilisateur.getEmail());

                if (firestoreUser.isPresent()) {
                    UtilisateurCollection existingUser = firestoreUser.get();
                    existingUser.setTentativesEchouees(0);
                    existingUser.setBloqueJusqua(null);
                    existingUser.setDateMiseAJour(utilisateurCollection.getDateMiseAJour());

                    utilisateurFirebaseService.update(existingUser.getDocId(), existingUser);
                    firestoreAvailable = true;
                }
            } catch (Exception e) {
                System.err.println("Erreur lors de la mise à jour dans Firestore: " + e.getMessage());
            }

            if (!firestoreAvailable) {
                String docId = utilisateur.getFirebaseUid() != null ?
                              utilisateur.getFirebaseUid() :
                              String.valueOf(utilisateur.getId());

                journalService.journaliser(
                    "UtilisateurCollection",
                    "UPDATE",
                    docId,
                    utilisateurCollection,
                    utilisateur.getVersion()
                );
            }
        }
    }

    private void handleFailedLogin(Utilisateur utilisateur) {
        Integer maxFailedAttempts = parametreService.getValeurAsInteger("MAX_FAILED_ATTEMPTS");
        Integer blockDurationMinutes = parametreService.getValeurAsInteger("BLOCK_DURATION_MINUTES");

        if (maxFailedAttempts == null) maxFailedAttempts = 5;
        if (blockDurationMinutes == null) blockDurationMinutes = 30;

        int tentatives = utilisateur.getTentativesEchouees() != null ? utilisateur.getTentativesEchouees() : 0;
        tentatives++;
        utilisateur.setTentativesEchouees(tentatives);

        if (tentatives >= maxFailedAttempts) {
            utilisateur.setBloqueJusqua(LocalDateTime.now().plusMinutes(blockDurationMinutes));
        }

        utilisateur.setDateMisAJour(LocalDateTime.now());

        Utilisateur savedUser = utilisateurRepository.save(utilisateur);
        UtilisateurCollection utilisateurCollection = convertToCollection(savedUser);

        boolean firestoreAvailable = false;
        try {
            Optional<UtilisateurCollection> firestoreUser = utilisateurFirebaseService.findByEmail(utilisateur.getEmail());

            if (firestoreUser.isPresent()) {
                UtilisateurCollection existingUser = firestoreUser.get();
                existingUser.setTentativesEchouees(utilisateurCollection.getTentativesEchouees());
                existingUser.setBloqueJusqua(utilisateurCollection.getBloqueJusqua());
                existingUser.setDateMiseAJour(utilisateurCollection.getDateMiseAJour());

                utilisateurFirebaseService.update(existingUser.getDocId(), existingUser);
                firestoreAvailable = true;
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la mise à jour dans Firestore: " + e.getMessage());
        }

        if (!firestoreAvailable) {
            String docId = utilisateur.getDocId();

            journalService.journaliser(
                "UtilisateurCollection",
                "UPDATE",
                docId,
                utilisateurCollection,
                utilisateur.getVersion()
            );
        }
    }

    private UtilisateurCollection convertToCollection(Utilisateur utilisateur) {
        UtilisateurCollection collection = new UtilisateurCollection();
        collection.setEmail(utilisateur.getEmail());
        collection.setNom(utilisateur.getNom());
        collection.setFirebaseUid(utilisateur.getFirebaseUid());
        collection.setActif(utilisateur.getActif() != null ? utilisateur.getActif() : false);
        collection.setTentativesEchouees(utilisateur.getTentativesEchouees() != null ? utilisateur.getTentativesEchouees() : 0);
        collection.setVersion(utilisateur.getVersion());
        collection.setId(utilisateur.getId());
        collection.setSynchronise(false);

        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
        if (utilisateur.getBloqueJusqua() != null) {
            collection.setBloqueJusqua(utilisateur.getBloqueJusqua().format(formatter));
        }
        if (utilisateur.getDateCreation() != null) {
            collection.setDateCreation(utilisateur.getDateCreation().format(formatter));
        }
        if (utilisateur.getDateMisAJour() != null) {
            collection.setDateMiseAJour(utilisateur.getDateMisAJour().format(formatter));
        }

        List<RoleUtilisateur> rolesUtilisateur = roleUtilisateurRepository.findByIdUtilisateur(utilisateur);
        if (!rolesUtilisateur.isEmpty()) {
            collection.setRole(rolesUtilisateur.get(0).getIdRole().getNom());
        }

        return collection;
    }

    public Utilisateur updateFirebaseIds(UtilisateurCollection col) throws Exception {
        if(col.getFirebaseUid() == null || col.getDocId() == null) {
            return null;
        }
        Optional<Utilisateur> uOpt = this.utilisateurRepository.findById(col.getId());
        if(uOpt.isPresent()) {
            Utilisateur u = uOpt.get();
            u.setDocId(col.getDocId());
            u.setFirebaseUid(col.getFirebaseUid());
            return this.utilisateurRepository.save(u);
        }
        throw new Exception("[UtilisateurService.updateFirebaseIds]: Utilisateur introuvable pour l'id: "+col.getId());
    }

    public Optional<Utilisateur> findByEmail(String email) {
        return this.utilisateurRepository.findByEmail(email);
    }
}
