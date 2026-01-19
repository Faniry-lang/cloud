package itu.cloud.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * Service Firestore pour la synchronisation des donnees avec Firebase.
 * Gere les operations CRUD sur Firestore et la synchronisation bidirectionnelle.
 */
@Service
public class FirestoreService {

    private static final String COLLECTION_USERS = "utilisateurs";
    private static final String COLLECTION_JOURNAL = "journal";
    private static final String COLLECTION_ROLES = "roles";
    private static final String COLLECTION_STATUTS = "statuts";
    private static final String COLLECTION_PARAMETRES = "parametres";

    /**
     * Obtient l'instance Firestore
     */
    private Firestore getFirestore() {
        return FirestoreClient.getFirestore();
    }

    // ==================== UTILISATEURS ====================

    /**
     * Cree ou met a jour un utilisateur dans Firestore
     */
    public void saveUtilisateur(Integer id, String email, String nom, String firebaseUid,
                                 Integer version, Instant dateCreation) {
        try {
            Firestore db = getFirestore();
            DocumentReference docRef = db.collection(COLLECTION_USERS).document(String.valueOf(id));

            Map<String, Object> data = new HashMap<>();
            data.put("id", id);
            data.put("email", email);
            data.put("nom", nom);
            data.put("firebaseUid", firebaseUid);
            data.put("version", version);
            data.put("dateCreation", dateCreation != null ? dateCreation.toString() : null);
            data.put("dateMiseAJour", Instant.now().toString());
            data.put("synchronise", true);

            docRef.set(data, SetOptions.merge()).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Erreur lors de la sauvegarde utilisateur dans Firestore: " + e.getMessage(), e);
        }
    }

    /**
     * Recupere un utilisateur depuis Firestore par ID
     */
    public Optional<Map<String, Object>> getUtilisateur(Integer id) {
        try {
            Firestore db = getFirestore();
            DocumentSnapshot document = db.collection(COLLECTION_USERS)
                    .document(String.valueOf(id))
                    .get()
                    .get();

            if (document.exists()) {
                return Optional.ofNullable(document.getData());
            }
            return Optional.empty();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Erreur lors de la lecture utilisateur depuis Firestore: " + e.getMessage(), e);
        }
    }

    /**
     * Recupere un utilisateur depuis Firestore par email
     */
    public Optional<Map<String, Object>> getUtilisateurByEmail(String email) {
        try {
            Firestore db = getFirestore();
            QuerySnapshot querySnapshot = db.collection(COLLECTION_USERS)
                    .whereEqualTo("email", email)
                    .limit(1)
                    .get()
                    .get();

            if (!querySnapshot.isEmpty()) {
                return Optional.ofNullable(querySnapshot.getDocuments().get(0).getData());
            }
            return Optional.empty();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Erreur lors de la recherche utilisateur par email: " + e.getMessage(), e);
        }
    }

    /**
     * Recupere tous les utilisateurs depuis Firestore
     */
    public List<Map<String, Object>> getAllUtilisateurs() {
        try {
            Firestore db = getFirestore();
            QuerySnapshot querySnapshot = db.collection(COLLECTION_USERS).get().get();

            List<Map<String, Object>> users = new ArrayList<>();
            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                if (doc.getData() != null) {
                    users.add(doc.getData());
                }
            }
            return users;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Erreur lors de la recuperation des utilisateurs: " + e.getMessage(), e);
        }
    }

    // ==================== JOURNAL ====================

    /**
     * Enregistre une entree de journal dans Firestore
     */
    public void saveJournalEntry(Integer localId, Integer idEntite, String typeEntite,
                                  String operation, Map<String, Object> donnees,
                                  Integer version, Instant dateCreation) {
        try {
            Firestore db = getFirestore();
            DocumentReference docRef = db.collection(COLLECTION_JOURNAL).document(String.valueOf(localId));

            Map<String, Object> data = new HashMap<>();
            data.put("localId", localId);
            data.put("idEntite", idEntite);
            data.put("typeEntite", typeEntite);
            data.put("operation", operation);
            data.put("donnees", donnees);
            data.put("version", version);
            data.put("dateCreation", dateCreation != null ? dateCreation.toString() : Instant.now().toString());
            data.put("synchronise", true);

            docRef.set(data).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Erreur lors de la sauvegarde journal dans Firestore: " + e.getMessage(), e);
        }
    }

    /**
     * Recupere les entrees de journal non synchronisees depuis Firestore
     */
    public List<Map<String, Object>> getJournalEntriesAfter(Instant since) {
        try {
            Firestore db = getFirestore();
            QuerySnapshot querySnapshot = db.collection(COLLECTION_JOURNAL)
                    .whereGreaterThan("dateCreation", since.toString())
                    .get()
                    .get();

            List<Map<String, Object>> entries = new ArrayList<>();
            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                if (doc.getData() != null) {
                    entries.add(doc.getData());
                }
            }
            return entries;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Erreur lors de la recuperation du journal: " + e.getMessage(), e);
        }
    }

    // ==================== ROLES ====================

    /**
     * Sauvegarde un role dans Firestore
     */
    public void saveRole(Integer id, String nom, Instant dateCreation) {
        try {
            Firestore db = getFirestore();
            DocumentReference docRef = db.collection(COLLECTION_ROLES).document(String.valueOf(id));

            Map<String, Object> data = new HashMap<>();
            data.put("id", id);
            data.put("nom", nom);
            data.put("dateCreation", dateCreation != null ? dateCreation.toString() : null);
            data.put("dateMiseAJour", Instant.now().toString());

            docRef.set(data, SetOptions.merge()).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Erreur lors de la sauvegarde role dans Firestore: " + e.getMessage(), e);
        }
    }

    // ==================== STATUTS ====================

    /**
     * Sauvegarde un statut dans Firestore
     */
    public void saveStatut(Integer id, String description, Instant dateCreation) {
        try {
            Firestore db = getFirestore();
            DocumentReference docRef = db.collection(COLLECTION_STATUTS).document(String.valueOf(id));

            Map<String, Object> data = new HashMap<>();
            data.put("id", id);
            data.put("description", description);
            data.put("dateCreation", dateCreation != null ? dateCreation.toString() : null);
            data.put("dateMiseAJour", Instant.now().toString());

            docRef.set(data, SetOptions.merge()).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Erreur lors de la sauvegarde statut dans Firestore: " + e.getMessage(), e);
        }
    }

    // ==================== PARAMETRES ====================

    /**
     * Sauvegarde un parametre dans Firestore
     */
    public void saveParametre(Integer id, String nom, String valeur, String type) {
        try {
            Firestore db = getFirestore();
            DocumentReference docRef = db.collection(COLLECTION_PARAMETRES).document(String.valueOf(id));

            Map<String, Object> data = new HashMap<>();
            data.put("id", id);
            data.put("nom", nom);
            data.put("valeur", valeur);
            data.put("type", type);
            data.put("dateMiseAJour", Instant.now().toString());

            docRef.set(data, SetOptions.merge()).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Erreur lors de la sauvegarde parametre dans Firestore: " + e.getMessage(), e);
        }
    }

    /**
     * Recupere tous les parametres depuis Firestore
     */
    public List<Map<String, Object>> getAllParametres() {
        try {
            Firestore db = getFirestore();
            QuerySnapshot querySnapshot = db.collection(COLLECTION_PARAMETRES).get().get();

            List<Map<String, Object>> params = new ArrayList<>();
            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                if (doc.getData() != null) {
                    params.add(doc.getData());
                }
            }
            return params;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Erreur lors de la recuperation des parametres: " + e.getMessage(), e);
        }
    }

    // ==================== UTILITY ====================

    /**
     * Verifie si Firestore est accessible
     */
    public boolean isAvailable() {
        try {
            Firestore db = getFirestore();
            db.collection("_health_check").document("ping").get().get();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

