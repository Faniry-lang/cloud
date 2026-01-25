package itu.cloud.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import itu.cloud.entities.*;
import itu.cloud.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * Service de synchronisation bidirectionnelle entre PostgreSQL local et Firestore.
 * Gere la logique de versionnage et la resolution des conflits.
 * Cree les comptes Firebase Auth pour les utilisateurs locaux sans firebase_uid.
 */
@Service
public class SyncService {

    private final UtilisateurRepository utilisateurRepository;
    private final RoleRepository roleRepository;
    private final StatutRepository statutRepository;
    private final ParametreRepository parametreRepository;
    private final JournalRepository journalRepository;
    private final SignalementRepository signalementRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final FirestoreService firestoreService;
    private final FirebaseAuthService firebaseAuthService;
    private final JournalService journalService;
    private final ObjectMapper objectMapper;

    public SyncService(UtilisateurRepository utilisateurRepository,
                       RoleRepository roleRepository,
                       StatutRepository statutRepository,
                       ParametreRepository parametreRepository,
                       JournalRepository journalRepository,
                       SignalementRepository signalementRepository,
                       EntrepriseRepository entrepriseRepository,
                       FirestoreService firestoreService,
                       FirebaseAuthService firebaseAuthService,
                       JournalService journalService,
                       ObjectMapper objectMapper) {
        this.utilisateurRepository = utilisateurRepository;
        this.roleRepository = roleRepository;
        this.statutRepository = statutRepository;
        this.parametreRepository = parametreRepository;
        this.journalRepository = journalRepository;
        this.signalementRepository = signalementRepository;
        this.entrepriseRepository = entrepriseRepository;
        this.firestoreService = firestoreService;
        this.firebaseAuthService = firebaseAuthService;
        this.journalService = journalService;
        this.objectMapper = objectMapper;
    }

    /**
     * Resultat de la synchronisation
     */
    public static class SyncResult {
        private int localToRemote = 0;
        private int remoteToLocal = 0;
        private int firebaseUsersCreated = 0;
        private int conflicts = 0;
        private List<String> errors = new ArrayList<>();
        private List<String> details = new ArrayList<>();

        public void incrementLocalToRemote() { localToRemote++; }
        public void incrementRemoteToLocal() { remoteToLocal++; }
        public void incrementFirebaseUsersCreated() { firebaseUsersCreated++; }
        public void incrementConflicts() { conflicts++; }
        public void addError(String error) { errors.add(error); }
        public void addDetail(String detail) { details.add(detail); }

        public int getLocalToRemote() { return localToRemote; }
        public int getRemoteToLocal() { return remoteToLocal; }
        public int getFirebaseUsersCreated() { return firebaseUsersCreated; }
        public int getConflicts() { return conflicts; }
        public List<String> getErrors() { return errors; }
        public List<String> getDetails() { return details; }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("localToRemote", localToRemote);
            map.put("remoteToLocal", remoteToLocal);
            map.put("firebaseUsersCreated", firebaseUsersCreated);
            map.put("conflicts", conflicts);
            map.put("errors", errors);
            map.put("details", details);
            map.put("success", errors.isEmpty());
            return map;
        }
    }

    /**
     * Synchronise toutes les donnees entre local et Firestore
     */
    @Transactional
    public SyncResult synchroniserTout() {
        SyncResult result = new SyncResult();

        // Verifier la disponibilite de Firestore
        if (!firestoreService.isAvailable()) {
            result.addError("Firestore n'est pas accessible");
            return result;
        }

        try {
            // 1. Synchroniser les entrees du journal non synchronisees (local -> remote)
            syncJournalToFirestore(result);

            // 2. Synchroniser les utilisateurs (avec creation Firebase Auth si necessaire)
            syncUtilisateursToFirestore(result);

            // 3. Synchroniser les roles
            syncRolesToFirestore(result);

            // 4. Synchroniser les statuts
            syncStatutsToFirestore(result);

            // 5. Synchroniser les parametres
            syncParametresToFirestore(result);

            // 6. Synchroniser les entreprises
            syncEntreprisesToFirestore(result);

            // 7. Synchroniser les signalements
            syncSignalementsToFirestore(result);

            result.addDetail("Synchronisation terminee avec succes");
        } catch (Exception e) {
            result.addError("Erreur lors de la synchronisation: " + e.getMessage());
        }

        return result;
    }

    /**
     * Synchronise uniquement les utilisateurs
     */
    @Transactional
    public SyncResult synchroniserUtilisateurs() {
        SyncResult result = new SyncResult();

        if (!firestoreService.isAvailable()) {
            result.addError("Firestore n'est pas accessible");
            return result;
        }

        try {
            syncUtilisateursToFirestore(result);
        } catch (Exception e) {
            result.addError("Erreur: " + e.getMessage());
        }

        return result;
    }

    /**
     * Synchronise le journal local vers Firestore
     */
    private void syncJournalToFirestore(SyncResult result) {
        List<Journal> nonSynchronises = journalRepository.findBySynchroniseFalse();

        for (Journal entry : nonSynchronises) {
            try {
                Map<String, Object> donnees = parseJsonToMap(entry.getDonnees());

                firestoreService.saveJournalEntry(
                    entry.getId(),
                    entry.getIdEntite(),
                    entry.getTypeEntite(),
                    entry.getOperation(),
                    donnees,
                    entry.getVersion(),
                    entry.getDateCreation()
                );

                // Marquer comme synchronise
                entry.setSynchronise(true);
                journalRepository.save(entry);

                result.incrementLocalToRemote();
                result.addDetail("Journal #" + entry.getId() + " synchronise");
            } catch (Exception e) {
                result.addError("Erreur sync journal #" + entry.getId() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Synchronise les utilisateurs vers Firestore.
     * Cree les comptes Firebase Auth pour les utilisateurs locaux sans firebase_uid.
     */
    private void syncUtilisateursToFirestore(SyncResult result) {
        List<Utilisateur> utilisateurs = utilisateurRepository.findAll();

        for (Utilisateur user : utilisateurs) {
            if (user.getDateSuppression() != null) continue;

            try {
                // Si l'utilisateur n'a pas de firebase_uid, creer le compte Firebase Auth
                if (user.getFirebaseUid() == null || user.getFirebaseUid().isBlank()) {
                    String firebaseUid = createFirebaseAccountForUser(user, result);
                    if (firebaseUid != null) {
                        user.setFirebaseUid(firebaseUid);
                        user.setVersion(user.getVersion() + 1);
                        user.setDateMisAJour(Instant.now());
                        utilisateurRepository.save(user);
                        result.incrementFirebaseUsersCreated();
                        result.addDetail("Compte Firebase cree pour " + user.getEmail() + " (uid: " + firebaseUid + ")");
                    }
                }

                // Verifier s'il existe deja dans Firestore
                Optional<Map<String, Object>> remoteUser = firestoreService.getUtilisateur(user.getId());

                if (remoteUser.isPresent()) {
                    // Comparer les versions
                    Object remoteVersionObj = remoteUser.get().get("version");
                    Integer remoteVersion = remoteVersionObj != null ? ((Number) remoteVersionObj).intValue() : null;
                    Integer localVersion = user.getVersion();

                    if (remoteVersion != null && localVersion != null && remoteVersion > localVersion) {
                        // Remote est plus recent - conflit, on garde le remote pour l'instant
                        result.incrementConflicts();
                        result.addDetail("Conflit utilisateur #" + user.getId() + " - version remote plus recente");
                        continue;
                    }
                }

                // Envoyer vers Firestore
                firestoreService.saveUtilisateur(
                    user.getId(),
                    user.getEmail(),
                    user.getNom(),
                    user.getFirebaseUid(),
                    user.getVersion(),
                    user.getDateCreation()
                );

                result.incrementLocalToRemote();
                result.addDetail("Utilisateur #" + user.getId() + " (" + user.getEmail() + ") synchronise");
            } catch (Exception e) {
                result.addError("Erreur sync utilisateur #" + user.getId() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Cree un compte Firebase Auth pour un utilisateur local.
     * Note: Necessite que l'utilisateur ait un mot de passe stocke localement.
     * Si le mot de passe n'est pas disponible, genere un mot de passe temporaire.
     */
    private String createFirebaseAccountForUser(Utilisateur user, SyncResult result) {
        try {
            // On ne peut pas recuperer le mot de passe original (hash)
            // On cree le compte avec un mot de passe temporaire
            // L'utilisateur devra faire "mot de passe oublie" pour le reinitialiser
            String tempPassword = generateTemporaryPassword();

            String firebaseUid = firebaseAuthService.createUser(user.getEmail(), tempPassword);

            if (firebaseUid != null) {
                result.addDetail("Note: Mot de passe temporaire genere pour " + user.getEmail() +
                    ". L'utilisateur doit reinitialiser son mot de passe Firebase.");
            }

            return firebaseUid;
        } catch (Exception e) {
            result.addError("Impossible de creer le compte Firebase pour " + user.getEmail() + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Genere un mot de passe temporaire securise
     */
    private String generateTemporaryPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%";
        StringBuilder sb = new StringBuilder();
        java.util.Random random = new java.security.SecureRandom();
        for (int i = 0; i < 16; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * Synchronise les roles vers Firestore
     */
    private void syncRolesToFirestore(SyncResult result) {
        List<Role> roles = roleRepository.findAll();

        for (Role role : roles) {
            if (role.getDateSuppression() != null) continue;

            try {
                firestoreService.saveRole(role.getId(), role.getNom(), role.getDateCreation());
                result.incrementLocalToRemote();
                result.addDetail("Role #" + role.getId() + " (" + role.getNom() + ") synchronise");
            } catch (Exception e) {
                result.addError("Erreur sync role #" + role.getId() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Synchronise les statuts vers Firestore
     */
    private void syncStatutsToFirestore(SyncResult result) {
        List<Statut> statuts = statutRepository.findAll();

        for (Statut statut : statuts) {
            if (statut.getDateSuppression() != null) continue;

            try {
                firestoreService.saveStatut(statut.getId(), statut.getDescription(), statut.getDateCreation());
                result.incrementLocalToRemote();
                result.addDetail("Statut #" + statut.getId() + " (" + statut.getDescription() + ") synchronise");
            } catch (Exception e) {
                result.addError("Erreur sync statut #" + statut.getId() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Synchronise les parametres vers Firestore
     */
    private void syncParametresToFirestore(SyncResult result) {
        List<Parametre> parametres = parametreRepository.findAll();

        for (Parametre param : parametres) {
            if (param.getDateSuppression() != null) continue;

            try {
                firestoreService.saveParametre(param.getId(), param.getNom(), param.getValeur(), param.getType());
                result.incrementLocalToRemote();
                result.addDetail("Parametre #" + param.getId() + " (" + param.getNom() + ") synchronise");
            } catch (Exception e) {
                result.addError("Erreur sync parametre #" + param.getId() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Synchronise les entreprises vers Firestore
     */
    private void syncEntreprisesToFirestore(SyncResult result) {
        List<Entreprise> entreprises = entrepriseRepository.findAll();

        for (Entreprise entreprise : entreprises) {
            if (entreprise.getDateSuppression() != null) continue;

            try {
                firestoreService.saveEntreprise(
                    entreprise.getId(),
                    entreprise.getNom(),
                    1, // version par defaut
                    entreprise.getDateCreation()
                );
                result.incrementLocalToRemote();
                result.addDetail("Entreprise #" + entreprise.getId() + " (" + entreprise.getNom() + ") synchronisee");
            } catch (Exception e) {
                result.addError("Erreur sync entreprise #" + entreprise.getId() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Synchronise les signalements vers Firestore
     */
    private void syncSignalementsToFirestore(SyncResult result) {
        List<Signalement> signalements = signalementRepository.findAll();

        for (Signalement signalement : signalements) {
            if (signalement.getDateSuppression() != null) continue;

            try {
                // Verifier s'il existe deja dans Firestore
                Optional<Map<String, Object>> remoteSignalement = firestoreService.getSignalement(signalement.getId());

                if (remoteSignalement.isPresent()) {
                    // Comparer les versions
                    Object remoteVersionObj = remoteSignalement.get().get("version");
                    Integer remoteVersion = remoteVersionObj != null ? ((Number) remoteVersionObj).intValue() : null;
                    Integer localVersion = signalement.getVersion();

                    if (remoteVersion != null && localVersion != null && remoteVersion > localVersion) {
                        // Remote est plus recent - conflit
                        result.incrementConflicts();
                        result.addDetail("Conflit signalement #" + signalement.getId() + " - version remote plus recente");
                        continue;
                    }
                }

                // Envoyer vers Firestore
                firestoreService.saveSignalement(
                    signalement.getId(),
                    signalement.getDescription(),
                    signalement.getSurfaceM2(),
                    signalement.getBudget(),
                    signalement.getIdEntreprise() != null ? signalement.getIdEntreprise().getId() : null,
                    signalement.getVersion(),
                    signalement.getDateCreation()
                );

                result.incrementLocalToRemote();
                result.addDetail("Signalement #" + signalement.getId() + " synchronise");
            } catch (Exception e) {
                result.addError("Erreur sync signalement #" + signalement.getId() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Synchronise depuis Firestore vers local (pull)
     */
    @Transactional
    public SyncResult synchroniserDepuisFirestore() {
        SyncResult result = new SyncResult();

        if (!firestoreService.isAvailable()) {
            result.addError("Firestore n'est pas accessible");
            return result;
        }

        try {
            // 1. Importer les utilisateurs
            pullUtilisateursFromFirestore(result);

            // 2. Importer les entreprises
            pullEntreprisesFromFirestore(result);

            // 3. Importer les signalements
            pullSignalementsFromFirestore(result);

            result.addDetail("Import depuis Firestore termine");
        } catch (Exception e) {
            result.addError("Erreur lors de l'import: " + e.getMessage());
        }

        return result;
    }

    /**
     * Importe les utilisateurs depuis Firestore
     */
    private void pullUtilisateursFromFirestore(SyncResult result) {
        List<Map<String, Object>> remoteUsers = firestoreService.getAllUtilisateurs();

        for (Map<String, Object> remoteUser : remoteUsers) {
            try {
                Integer remoteId = ((Number) remoteUser.get("id")).intValue();
                String email = (String) remoteUser.get("email");
                Integer remoteVersion = remoteUser.get("version") != null ?
                    ((Number) remoteUser.get("version")).intValue() : 1;

                Optional<Utilisateur> localUser = utilisateurRepository.findById(remoteId);

                if (localUser.isEmpty()) {
                    // Creer localement
                    Utilisateur newUser = new Utilisateur();
                    newUser.setEmail(email);
                    newUser.setNom((String) remoteUser.get("nom"));
                    newUser.setFirebaseUid((String) remoteUser.get("firebaseUid"));
                    newUser.setVersion(remoteVersion);
                    newUser.setActif(true);
                    newUser.setTentativesEchouees(0);
                    newUser.setDateCreation(Instant.now());

                    utilisateurRepository.save(newUser);
                    result.incrementRemoteToLocal();
                    result.addDetail("Utilisateur " + email + " importe depuis Firestore");
                } else {
                    // Verifier la version
                    Utilisateur local = localUser.get();
                    if (remoteVersion > local.getVersion()) {
                        // Mettre a jour depuis remote
                        local.setNom((String) remoteUser.get("nom"));
                        local.setFirebaseUid((String) remoteUser.get("firebaseUid"));
                        local.setVersion(remoteVersion);
                        local.setDateMisAJour(Instant.now());

                        utilisateurRepository.save(local);
                        result.incrementRemoteToLocal();
                        result.addDetail("Utilisateur " + email + " mis a jour depuis Firestore");
                    }
                }
            } catch (Exception e) {
                result.addError("Erreur import utilisateur: " + e.getMessage());
            }
        }
    }

    /**
     * Importe les entreprises depuis Firestore
     */
    private void pullEntreprisesFromFirestore(SyncResult result) {
        List<Map<String, Object>> remoteEntreprises = firestoreService.getAllEntreprises();

        for (Map<String, Object> remoteEntreprise : remoteEntreprises) {
            try {
                Integer remoteId = ((Number) remoteEntreprise.get("id")).intValue();
                String nom = (String) remoteEntreprise.get("nom");

                Optional<Entreprise> localEntreprise = entrepriseRepository.findById(remoteId);

                if (localEntreprise.isEmpty()) {
                    // Creer localement
                    Entreprise newEntreprise = new Entreprise();
                    newEntreprise.setNom(nom);
                    newEntreprise.setDateCreation(Instant.now());

                    entrepriseRepository.save(newEntreprise);
                    result.incrementRemoteToLocal();
                    result.addDetail("Entreprise " + nom + " importee depuis Firestore");
                } else {
                    // Mettre a jour si necessaire
                    Entreprise local = localEntreprise.get();
                    if (!nom.equals(local.getNom())) {
                        local.setNom(nom);
                        local.setDateMisAJour(Instant.now());
                        entrepriseRepository.save(local);
                        result.incrementRemoteToLocal();
                        result.addDetail("Entreprise " + nom + " mise a jour depuis Firestore");
                    }
                }
            } catch (Exception e) {
                result.addError("Erreur import entreprise: " + e.getMessage());
            }
        }
    }

    /**
     * Importe les signalements depuis Firestore
     */
    private void pullSignalementsFromFirestore(SyncResult result) {
        List<Map<String, Object>> remoteSignalements = firestoreService.getAllSignalements();

        for (Map<String, Object> remoteSignalement : remoteSignalements) {
            try {
                Integer remoteId = ((Number) remoteSignalement.get("id")).intValue();
                Integer remoteVersion = remoteSignalement.get("version") != null ?
                    ((Number) remoteSignalement.get("version")).intValue() : 1;

                Optional<Signalement> localSignalement = signalementRepository.findById(remoteId);

                if (localSignalement.isEmpty()) {
                    // Creer localement
                    Signalement newSignalement = new Signalement();
                    newSignalement.setDescription((String) remoteSignalement.get("description"));

                    // Convertir surfaceM2
                    if (remoteSignalement.get("surfaceM2") != null) {
                        newSignalement.setSurfaceM2(new BigDecimal(remoteSignalement.get("surfaceM2").toString()));
                    }

                    // Convertir budget
                    if (remoteSignalement.get("budget") != null) {
                        newSignalement.setBudget(new BigDecimal(remoteSignalement.get("budget").toString()));
                    }

                    // Lier l'entreprise
                    if (remoteSignalement.get("idEntreprise") != null) {
                        Integer idEntreprise = ((Number) remoteSignalement.get("idEntreprise")).intValue();
                        Optional<Entreprise> entreprise = entrepriseRepository.findById(idEntreprise);
                        if (entreprise.isPresent()) {
                            newSignalement.setIdEntreprise(entreprise.get());
                        }
                    }

                    newSignalement.setVersion(remoteVersion);
                    newSignalement.setDateCreation(Instant.now());

                    signalementRepository.save(newSignalement);
                    result.incrementRemoteToLocal();
                    result.addDetail("Signalement #" + remoteId + " importe depuis Firestore");
                } else {
                    // Verifier la version
                    Signalement local = localSignalement.get();
                    if (remoteVersion > local.getVersion()) {
                        // Mettre a jour depuis remote
                        local.setDescription((String) remoteSignalement.get("description"));

                        if (remoteSignalement.get("surfaceM2") != null) {
                            local.setSurfaceM2(new BigDecimal(remoteSignalement.get("surfaceM2").toString()));
                        }
                        if (remoteSignalement.get("budget") != null) {
                            local.setBudget(new BigDecimal(remoteSignalement.get("budget").toString()));
                        }

                        local.setVersion(remoteVersion);
                        local.setDateMisAJour(Instant.now());

                        signalementRepository.save(local);
                        result.incrementRemoteToLocal();
                        result.addDetail("Signalement #" + remoteId + " mis a jour depuis Firestore");
                    }
                }
            } catch (Exception e) {
                result.addError("Erreur import signalement: " + e.getMessage());
            }
        }
    }

    /**
     * Verifie le statut de synchronisation
     */
    public Map<String, Object> getStatutSynchronisation() {
        Map<String, Object> status = new HashMap<>();

        // Compter les entrees non synchronisees
        long journalNonSync = journalRepository.countBySynchroniseFalse();

        status.put("journalEnAttente", journalNonSync);
        status.put("firestoreDisponible", firestoreService.isAvailable());
        status.put("derniereVerification", Instant.now().toString());

        return status;
    }

    /**
     * Parse une chaine JSON en Map
     */
    private Map<String, Object> parseJsonToMap(String json) {
        if (json == null || json.isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            return new HashMap<>();
        }
    }
}

