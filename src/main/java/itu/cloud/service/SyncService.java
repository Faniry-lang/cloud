package itu.cloud.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import itu.cloud.entities.*;
import itu.cloud.repositories.*;
import itu.cloud.helpers.ConversionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * Service de synchronisation bidirectionnelle entre PostgreSQL local et Firestore.
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
    private final DataSource dataSource;

    @PersistenceContext
    private EntityManager entityManager;

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
                       ObjectMapper objectMapper,
                       DataSource dataSource) {
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
        this.dataSource = dataSource;
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
                    Integer remoteVersion = remoteVersionObj != null ? ConversionUtils.toInteger(remoteVersionObj) : null;
                    Integer localVersion = signalement.getVersion();

                    if (remoteVersion != null && localVersion != null && remoteVersion > localVersion) {
                        // Remote est plus recent - conflit
                        result.incrementConflicts();
                        result.addDetail("Conflit signalement #" + signalement.getId() + " - version remote plus recente");
                        continue;
                    }
                }

                // extraire lat/lng depuis la colonne points locale
                Double[] latLng = getLatLngFor(signalement.getId());

                // Envoyer vers Firestore (incluant lat/lng)
                firestoreService.saveSignalement(
                    signalement.getId(),
                    signalement.getDescription(),
                    signalement.getSurfaceM2(),
                    signalement.getBudget(),
                    signalement.getIdEntreprise() != null ? signalement.getIdEntreprise().getId() : null,
                    signalement.getVersion(),
                    signalement.getDateCreation(),
                    latLng[0],
                    latLng[1]
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
                Integer remoteId = ConversionUtils.toInteger(remoteUser.get("id"));
                String email = remoteUser.get("email") != null ? remoteUser.get("email").toString() : null;
                Integer remoteVersion = ConversionUtils.toInteger(remoteUser.get("version"));
                if (remoteVersion == null) remoteVersion = 1;

                if (email == null) {
                    result.addError("Utilisateur import ignore: email manquant: " + remoteUser);
                    continue;
                }

                Optional<Utilisateur> localUser = Optional.empty();
                if (remoteId != null) {
                    localUser = utilisateurRepository.findById(remoteId);
                } else {
                    // try to find by email
                    localUser = utilisateurRepository.findByEmail(email);
                }

                if (localUser.isEmpty()) {
                    // Creer localement
                    Utilisateur newUser = new Utilisateur();
                    newUser.setEmail(email);
                    newUser.setNom(remoteUser.get("nom") != null ? remoteUser.get("nom").toString() : null);
                    newUser.setFirebaseUid(remoteUser.get("firebaseUid") != null ? remoteUser.get("firebaseUid").toString() : null);
                    newUser.setVersion(remoteVersion);
                    newUser.setActif(true);
                    newUser.setTentativesEchouees(0);
                    newUser.setDateCreation(ConversionUtils.toInstant(remoteUser.get("dateCreation")) != null ? ConversionUtils.toInstant(remoteUser.get("dateCreation")) : Instant.now());

                    utilisateurRepository.save(newUser);
                    result.incrementRemoteToLocal();
                    result.addDetail("Utilisateur " + email + " importe depuis Firestore");
                } else {
                    // Verifier la version
                    Utilisateur local = localUser.get();
                    if (remoteVersion > (local.getVersion() != null ? local.getVersion() : 0)) {
                        // Mettre a jour depuis remote
                        local.setNom(remoteUser.get("nom") != null ? remoteUser.get("nom").toString() : local.getNom());
                        local.setFirebaseUid(remoteUser.get("firebaseUid") != null ? remoteUser.get("firebaseUid").toString() : local.getFirebaseUid());
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
                Integer remoteId = ConversionUtils.toInteger(remoteEntreprise.get("id"));
                String nom = (String) remoteEntreprise.get("nom");

                if (remoteId == null) {
                    // pas d'id distant -> creer localement
                    Entreprise newEntreprise = new Entreprise();
                    newEntreprise.setNom(nom);
                    newEntreprise.setDateCreation(Instant.now());
                    entrepriseRepository.save(newEntreprise);
                    result.incrementRemoteToLocal();
                    result.addDetail("Entreprise " + nom + " importee depuis Firestore (id absent)");
                    continue;
                }

                Optional<Entreprise> localEntreprise = entrepriseRepository.findById(remoteId);

                if (localEntreprise.isEmpty()) {
                    // Creer localement avec id non-preserve car JPA/sequence peut ne pas permettre
                    Entreprise newEntreprise = new Entreprise();
                    newEntreprise.setNom(nom);
                    newEntreprise.setDateCreation(Instant.now());
                    entrepriseRepository.save(newEntreprise);
                    result.incrementRemoteToLocal();
                    result.addDetail("Entreprise " + nom + " importee depuis Firestore");
                } else {
                    // Mettre a jour si necessaire
                    Entreprise local = localEntreprise.get();
                    if (nom != null && !nom.equals(local.getNom())) {
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
                // Utiliser postgres_id en priorité sinon id
                Object remoteIdObj = remoteSignalement.get("postgres_id");
                if (remoteIdObj == null) remoteIdObj = remoteSignalement.get("id");
                Integer remoteId = ConversionUtils.toInteger(remoteIdObj);

                Integer remoteVersion = ConversionUtils.toInteger(remoteSignalement.get("version"));
                if (remoteVersion == null) remoteVersion = 1;

                // Extract location if present
                Double[] latLng = extractLatLng(remoteSignalement.get("location"));
                Double latitude = latLng[0];
                Double longitude = latLng[1];

                Optional<Signalement> localSignalement = Optional.empty();
                if (remoteId != null) {
                    localSignalement = signalementRepository.findById(remoteId);
                }

                if (localSignalement.isEmpty()) {
                    // Validate required foreign key idEntreprise before creating
                    Integer idEntreprise = ConversionUtils.toInteger(remoteSignalement.get("idEntreprise"));
                    if (idEntreprise == null) {
                        result.addError("Signalement import ignore: idEntreprise manquant ou invalide pour remote data: " + remoteSignalement);
                        continue;
                    }

                    // Create via repository to avoid mixing native inserts and JPA identity handling
                    Signalement newSignalement = new Signalement();
                    newSignalement.setDescription(remoteSignalement.get("description") != null ? remoteSignalement.get("description").toString() : null);

                    BigDecimal surface = ConversionUtils.toBigDecimal(remoteSignalement.get("surfaceM2"));
                    if (surface != null) newSignalement.setSurfaceM2(surface);

                    BigDecimal budget = ConversionUtils.toBigDecimal(remoteSignalement.get("budget"));
                    if (budget != null) newSignalement.setBudget(budget);

                    // set entreprise (required)
                    Optional<Entreprise> entOpt = entrepriseRepository.findById(idEntreprise);
                    if (entOpt.isEmpty()) {
                        result.addError("Signalement import ignore: entreprise locale introuvable pour id=" + idEntreprise + ". Remote data: " + remoteSignalement);
                        // do not create the signalement to avoid DB constraint violations
                        continue;
                    }
                    newSignalement.setIdEntreprise(entOpt.get());

                    newSignalement.setVersion(remoteVersion);
                    Instant dateCreation = ConversionUtils.toInstant(remoteSignalement.get("dateCreation"));
                    newSignalement.setDateCreation(dateCreation != null ? dateCreation : Instant.now());

                    Signalement saved = signalementRepository.save(newSignalement);
                    Integer createdId = saved.getId();

                    result.incrementRemoteToLocal();
                    result.addDetail("Signalement local cree (id genere): " + createdId);

                    // Mettre a jour la colonne points si location presente
                    if (latitude != null && longitude != null) {
                        try {
                            updatePointsInDb(createdId, longitude, latitude, result);
                            result.addDetail("Points geographiques pour signalement #" + createdId + " mis a jour");
                        } catch (Exception e) {
                            result.addError("Impossible de mettre a jour points pour signalement #" + createdId + ": " + e.getMessage());
                        }
                    }

                } else {
                    // existing local: update if remote version newer
                    Signalement local = localSignalement.get();
                    if (remoteVersion > (local.getVersion() != null ? local.getVersion() : 0)) {
                        local.setDescription(remoteSignalement.get("description") != null ? remoteSignalement.get("description").toString() : local.getDescription());

                        BigDecimal surface = ConversionUtils.toBigDecimal(remoteSignalement.get("surfaceM2"));
                        if (surface != null) local.setSurfaceM2(surface);

                        BigDecimal budget = ConversionUtils.toBigDecimal(remoteSignalement.get("budget"));
                        if (budget != null) local.setBudget(budget);

                        Integer idEntreprise = ConversionUtils.toInteger(remoteSignalement.get("idEntreprise"));
                        if (idEntreprise != null) {
                            entrepriseRepository.findById(idEntreprise).ifPresent(local::setIdEntreprise);
                        } else {
                            // si idEntreprise invalide -> log et skip update to avoid DB constraint fail
                            result.addError("Signalement update ignore (idEntreprise invalide) pour remote: " + remoteSignalement);
                            continue;
                        }

                        local.setVersion(remoteVersion);
                        local.setDateMisAJour(Instant.now());

                        signalementRepository.save(local);
                        result.incrementRemoteToLocal();
                        result.addDetail("Signalement #" + (remoteId != null ? remoteId : local.getId()) + " mis a jour depuis Firestore");

                        // Mettre a jour points si location presente
                        if (latitude != null && longitude != null) {
                            try {
                                updatePointsInDb(local.getId(), longitude, latitude, result);
                                result.addDetail("Points geographiques pour signalement #" + local.getId() + " mis a jour");
                            } catch (Exception e) {
                                result.addError("Impossible de mettre a jour points pour signalement #" + local.getId() + ": " + e.getMessage());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // clear persistence context to avoid Hibernate AssertionFailure when a previous save partially failed
                try { entityManager.clear(); } catch (Exception ignored) {}
                result.addError("Erreur import signalement: " + e.getMessage());
            }
        }
    }

    /**
     * Extrait latitude/longitude depuis l'objet 'location' renvoyé par Firestore.
     * Supporte Map<String,Object> ou String formattée.
     * Retourne Double[]{lat, lng} ou {null, null} si absent.
     */
    private Double[] extractLatLng(Object locObj) {
        Double lat = null;
        Double lng = null;

        if (locObj == null) return new Double[]{null, null};

        if (locObj instanceof Map) {
            Map<?, ?> loc = (Map<?, ?>) locObj;
            Object latObj = loc.get("lat");
            Object lngObj = loc.get("lng");
            if (latObj instanceof Number) lat = ((Number) latObj).doubleValue();
            else if (latObj instanceof String) {
                try { lat = Double.parseDouble(((String) latObj).trim()); } catch (Exception ignored) {}
            }
            if (lngObj instanceof Number) lng = ((Number) lngObj).doubleValue();
            else if (lngObj instanceof String) {
                try { lng = Double.parseDouble(((String) lngObj).trim()); } catch (Exception ignored) {}
            }
            return new Double[]{lat, lng};
        }

        // Si c'est une String du type "{lng=47.56, lat=-18.87}" ou "lat=.., lng=.."
        if (locObj instanceof String) {
            String s = (String) locObj;
            try {
                // rechercher lat et lng à l'aide de regex
                java.util.regex.Pattern pLat = java.util.regex.Pattern.compile("lat\\s*=\\s*([-+]?[0-9]*\\.?[0-9]+)", java.util.regex.Pattern.CASE_INSENSITIVE);
                java.util.regex.Pattern pLng = java.util.regex.Pattern.compile("lng\\s*=\\s*([-+]?[0-9]*\\.?[0-9]+)", java.util.regex.Pattern.CASE_INSENSITIVE);
                java.util.regex.Matcher mLat = pLat.matcher(s);
                java.util.regex.Matcher mLng = pLng.matcher(s);
                if (mLat.find()) lat = Double.parseDouble(mLat.group(1));
                if (mLng.find()) lng = Double.parseDouble(mLng.group(1));
            } catch (Exception ignored) {}
        }

        return new Double[]{lat, lng};
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

    // helper to extract lat/lng from points geometry for a given signalement id
    private Double[] getLatLngFor(Integer id) {
        if (id == null) return new Double[]{null, null};
        String sql = "SELECT ST_Y(points) as lat, ST_X(points) as lng FROM signalements WHERE id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double lat = rs.getDouble("lat");
                    if (rs.wasNull()) return new Double[]{null, null};
                    double lng = rs.getDouble("lng");
                    if (rs.wasNull()) return new Double[]{null, null};
                    return new Double[]{lat, lng};
                }
            }
        } catch (SQLException e) {
            // ignore
        }
        return new Double[]{null, null};
    }

    // JDBC helper to update points using DataSource
    private void updatePointsInDb(Integer id, Double longitude, Double latitude, SyncResult result) {
        if (id == null || longitude == null || latitude == null) return;
        String sql = "UPDATE signalements SET points = ST_SetSRID(ST_MakePoint(?, ?), 4326) WHERE id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, longitude);
            ps.setDouble(2, latitude);
            ps.setInt(3, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            result.addError("Impossible de mettre a jour points (JDBC) pour signalement #" + id + ": " + e.getMessage());
        }
    }
}

