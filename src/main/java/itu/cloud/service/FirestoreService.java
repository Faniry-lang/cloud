package itu.cloud.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Service
public class FirestoreService {

    private static final String COLLECTION_USERS = "utilisateurs";
    private static final String COLLECTION_JOURNAL = "journal";
    private static final String COLLECTION_ROLES = "roles";
    private static final String COLLECTION_STATUTS = "statuts";
    private static final String COLLECTION_PARAMETRES = "parametres";
    private static final String COLLECTION_ENTREPRISES = "entreprises";
    private static final String COLLECTION_SIGNALEMENTS = "signalements";

    private Firestore getFirestore() {
        return FirestoreClient.getFirestore();
    }

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
     * Cree ou met a jour un utilisateur dans Firestore avec son role
     */
    public void saveUtilisateurWithRole(Integer id, String email, String nom, String firebaseUid,
                                        String role, Integer version, Instant dateCreation) {
        try {
            Firestore db = getFirestore();
            DocumentReference docRef = db.collection(COLLECTION_USERS).document(String.valueOf(id));

            Map<String, Object> data = new HashMap<>();
            data.put("id", id);
            data.put("email", email);
            data.put("nom", nom);
            data.put("firebaseUid", firebaseUid);
            data.put("role", role != null ? role : "MANAGER");
            data.put("version", version);
            data.put("dateCreation", dateCreation != null ? dateCreation.toString() : null);
            data.put("dateMiseAJour", Instant.now().toString());
            data.put("synchronise", true);

            docRef.set(data, SetOptions.merge()).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Erreur lors de la sauvegarde utilisateur avec role dans Firestore: " + e.getMessage(), e);
        }
    }

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

    public Optional<Map<String, Object>> getUtilisateurByEmail(String email) {
        try {
            Firestore db = getFirestore();
            QuerySnapshot querySnapshot = db.collection(COLLECTION_USERS)
                    .whereEqualTo("email", email)
                    .limit(1)
                    .get()
                    .get();

            if (!querySnapshot.isEmpty()) {
                DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                Map<String, Object> data = doc.getData();
                if (data == null) data = new HashMap<>();
                // include the firestore document id so callers can use it to update the exact doc
                data.put("firebase_id", doc.getId());
                return Optional.ofNullable(data);
            }
            return Optional.empty();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Erreur lors de la recherche utilisateur par email: " + e.getMessage(), e);
        }
    }

    /**
     * Sauvegarde un utilisateur en utilisant l'identifiant du document Firestore (docId).
     */
    public void saveUtilisateurByDocId(String docId, Integer id, String email, String nom, String firebaseUid,
                                       Integer version, Instant dateCreation) {
        try {
            Firestore db = getFirestore();
            DocumentReference docRef = db.collection(COLLECTION_USERS).document(docId);

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
            throw new RuntimeException("Erreur lors de la sauvegarde utilisateur dans Firestore (docId): " + e.getMessage(), e);
        }
    }

    /**
     * Sauvegarde un utilisateur avec role en utilisant l'identifiant du document Firestore (docId).
     */
    public void saveUtilisateurWithRoleByDocId(String docId, Integer id, String email, String nom, String firebaseUid,
                                               String role, Integer version, Instant dateCreation) {
        try {
            Firestore db = getFirestore();
            DocumentReference docRef = db.collection(COLLECTION_USERS).document(docId);

            Map<String, Object> data = new HashMap<>();
            data.put("id", id);
            data.put("email", email);
            data.put("nom", nom);
            data.put("firebaseUid", firebaseUid);
            data.put("role", role != null ? role : "MANAGER");
            data.put("version", version);
            data.put("dateCreation", dateCreation != null ? dateCreation.toString() : null);
            data.put("dateMiseAJour", Instant.now().toString());
            data.put("synchronise", true);

            docRef.set(data, SetOptions.merge()).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Erreur lors de la sauvegarde utilisateur avec role dans Firestore (docId): " + e.getMessage(), e);
        }
    }

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

    public List<Map<String, Object>> getAllRoles() {
        try {
            Firestore db = getFirestore();
            QuerySnapshot querySnapshot = db.collection(COLLECTION_ROLES).get().get();

            List<Map<String, Object>> roles = new ArrayList<>();
            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                if (doc.getData() != null) {
                    roles.add(doc.getData());
                }
            }
            return roles;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Erreur lors de la recuperation des roles: " + e.getMessage(), e);
        }
    }

    public Optional<Map<String, Object>> getRole(Integer id) {
        try {
            Firestore db = getFirestore();
            DocumentSnapshot document = db.collection(COLLECTION_ROLES)
                    .document(String.valueOf(id))
                    .get()
                    .get();

            if (document.exists()) {
                return Optional.ofNullable(document.getData());
            }
            return Optional.empty();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Erreur lors de la lecture role depuis Firestore: " + e.getMessage(), e);
        }
    }

    public void deleteRole(Integer id) {
        try {
            Firestore db = getFirestore();
            DocumentReference docRef = db.collection(COLLECTION_ROLES).document(String.valueOf(id));

            Map<String, Object> updates = new HashMap<>();
            updates.put("dateSuppression", Instant.now().toString());
            updates.put("dateMiseAJour", Instant.now().toString());

            docRef.update(updates).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Erreur lors de la suppression role dans Firestore: " + e.getMessage(), e);
        }
    }

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

    public List<Map<String, Object>> getAllStatuts() {
        try {
            Firestore db = getFirestore();
            QuerySnapshot querySnapshot = db.collection(COLLECTION_STATUTS).get().get();

            List<Map<String, Object>> statuts = new ArrayList<>();
            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                if (doc.getData() != null) {
                    statuts.add(doc.getData());
                }
            }
            return statuts;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Erreur lors de la recuperation des statuts: " + e.getMessage(), e);
        }
    }

    public Optional<Map<String, Object>> getStatut(Integer id) {
        try {
            Firestore db = getFirestore();
            DocumentSnapshot document = db.collection(COLLECTION_STATUTS)
                    .document(String.valueOf(id))
                    .get()
                    .get();

            if (document.exists()) {
                return Optional.ofNullable(document.getData());
            }
            return Optional.empty();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Erreur lors de la lecture statut depuis Firestore: " + e.getMessage(), e);
        }
    }


    public void deleteStatut(Integer id) {
        try {
            Firestore db = getFirestore();
            DocumentReference docRef = db.collection(COLLECTION_STATUTS).document(String.valueOf(id));

            Map<String, Object> updates = new HashMap<>();
            updates.put("dateSuppression", Instant.now().toString());
            updates.put("dateMiseAJour", Instant.now().toString());

            docRef.update(updates).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Erreur lors de la suppression statut dans Firestore: " + e.getMessage(), e);
        }
    }

    public void saveEntreprise(Integer id, String nom, int version, Instant dateCreation) {
        try {
            Firestore db = getFirestore();
            DocumentReference docRef = db.collection(COLLECTION_ENTREPRISES).document(String.valueOf(id));

            Map<String, Object> data = new HashMap<>();
            data.put("id", id);
            data.put("nom", nom);
            data.put("version", version);
            data.put("dateCreation", dateCreation != null ? dateCreation.toString() : null);
            data.put("dateMiseAJour", Instant.now().toString());

            docRef.set(data, SetOptions.merge()).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Erreur lors de la sauvegarde entreprise dans Firestore: " + e.getMessage(), e);
        }
    }

    public List<Map<String, Object>> getAllEntreprises() {
        try {
            Firestore db = getFirestore();
            QuerySnapshot querySnapshot = db.collection(COLLECTION_ENTREPRISES).get().get();

            List<Map<String, Object>> entreprises = new ArrayList<>();
            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                if (doc.getData() != null) {
                    Map<String, Object> data = doc.getData();
                    if (data.get("dateSuppression") == null) {
                        entreprises.add(data);
                    }
                }
            }
            return entreprises;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Erreur lors de la recuperation des entreprises: " + e.getMessage(), e);
        }
    }

    public Optional<Map<String, Object>> getEntreprise(Integer id) {
        try {
            Firestore db = getFirestore();
            DocumentSnapshot document = db.collection(COLLECTION_ENTREPRISES)
                    .document(String.valueOf(id))
                    .get()
                    .get();

            if (document.exists()) {
                Map<String, Object> data = document.getData();
                // Verifier si non supprimee
                if (data != null && data.get("dateSuppression") == null) {
                    return Optional.of(data);
                }
            }
            return Optional.empty();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Erreur lors de la lecture entreprise depuis Firestore: " + e.getMessage(), e);
        }
    }

    public void deleteEntreprise(Integer id) {
        try {
            Firestore db = getFirestore();
            DocumentReference docRef = db.collection(COLLECTION_ENTREPRISES).document(String.valueOf(id));

            Map<String, Object> updates = new HashMap<>();
            updates.put("dateSuppression", Instant.now().toString());
            updates.put("dateMiseAJour", Instant.now().toString());

            docRef.update(updates).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Erreur lors de la suppression entreprise dans Firestore: " + e.getMessage(), e);
        }
    }

    public void saveSignalement(String id, String description, BigDecimal surfaceM2,
                                BigDecimal budget, Integer idEntreprise, Integer version,
                                Instant dateCreation, Double latitude, Double longitude) {
        try {
            Firestore db = getFirestore();
            DocumentReference docRef = db.collection(COLLECTION_SIGNALEMENTS).document(id);

            Map<String, Object> data = new HashMap<>();
            // id remains the document id
            data.put("id", id);
            data.put("description", description);

            if (surfaceM2 != null) data.put("surfaceM2", surfaceM2.doubleValue());
            else data.put("surfaceM2", null);

            if (budget != null) data.put("budget", budget.doubleValue());
            else data.put("budget", null);

            data.put("idEntreprise", idEntreprise);
            data.put("version", version);
            data.put("dateCreation", dateCreation != null ? dateCreation.toString() : null);
            data.put("dateMiseAJour", Instant.now().toString());

            // location
            if (latitude != null && longitude != null) {
                Map<String, Object> loc = new HashMap<>();
                loc.put("lat", latitude);
                loc.put("lng", longitude);
                data.put("location", loc);
            }

            docRef.set(data, SetOptions.merge()).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Erreur lors de la sauvegarde signalement dans Firestore: " + e.getMessage(), e);
        }
    }

    /**
     * Cree ou met a jour un signalement dans Firestore
     * Ajoute postgres_id et la location si fournie.
     */
    public void saveSignalement(Integer id, String description, BigDecimal surfaceM2,
                                 BigDecimal budget, Integer idEntreprise, Integer version,
                                 Instant dateCreation, Double latitude, Double longitude) {
        try {
            Firestore db = getFirestore();
            DocumentReference docRef = db.collection(COLLECTION_SIGNALEMENTS).document(String.valueOf(id));

            Map<String, Object> data = new HashMap<>();
            // id remains the document id
            data.put("id", id);
            // keep postgres_id for compatibility
            data.put("postgres_id", id);
            data.put("description", description);

            if (surfaceM2 != null) data.put("surfaceM2", surfaceM2.doubleValue());
            else data.put("surfaceM2", null);

            if (budget != null) data.put("budget", budget.doubleValue());
            else data.put("budget", null);

            data.put("idEntreprise", idEntreprise);
            data.put("version", version);
            data.put("dateCreation", dateCreation != null ? dateCreation.toString() : null);
            data.put("dateMiseAJour", Instant.now().toString());

            // location
            if (latitude != null && longitude != null) {
                Map<String, Object> loc = new HashMap<>();
                loc.put("lat", latitude);
                loc.put("lng", longitude);
                data.put("location", loc);
            }

            docRef.set(data, SetOptions.merge()).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Erreur lors de la sauvegarde signalement dans Firestore: " + e.getMessage(), e);
        }
    }

    /**
     * Compatibility overload: ancienne signature sans latitude/longitude.
     */
    public void saveSignalement(Integer id, String description, BigDecimal surfaceM2,
                                 BigDecimal budget, Integer idEntreprise, Integer version,
                                 Instant dateCreation) {
        saveSignalement(id, description, surfaceM2, budget, idEntreprise, version, dateCreation, null, null);
    }

    public List<Map<String, Object>> getAllSignalements() {
        try {
            Firestore db = getFirestore();
            QuerySnapshot querySnapshot = db.collection(COLLECTION_SIGNALEMENTS).get().get();

            List<Map<String, Object>> signalements = new ArrayList<>();
            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                if (doc.getData() != null) {
                    Map<String, Object> data = doc.getData();
                    data.put("firebase_id", doc.getId());

                    // Filtrer les signalements non supprimes
                    if (data.get("dateSuppression") == null) {
                        signalements.add(data);
                    }
                }
            }

            return signalements;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Erreur lors de la recuperation des signalements: " + e.getMessage(), e);
        }
    }

    public Optional<Map<String, Object>> getSignalement(Integer id) {
        try {
            Firestore db = getFirestore();
            DocumentSnapshot document = db.collection(COLLECTION_SIGNALEMENTS)
                    .document(String.valueOf(id))
                    .get()
                    .get();

            if (document.exists()) {
                Map<String, Object> data = document.getData();
                data.put("firebase_id", document.getId());

                if (data != null && data.get("dateSuppression") == null) {
                    return Optional.of(data);
                }
            }
            return Optional.empty();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Erreur lors de la lecture signalement depuis Firestore: " + e.getMessage(), e);
        }
    }

    public Optional<Map<String, Object>> getSignalementFromFirebase(String id) {
        try {
            Firestore db = getFirestore();
            DocumentSnapshot document = db.collection(COLLECTION_SIGNALEMENTS)
                    .document(id)
                    .get()
                    .get();

            if (document.exists()) {
                Map<String, Object> data = document.getData();
                data.put("firebase_id", document.getId());

                if (data != null && data.get("dateSuppression") == null) {
                    return Optional.of(data);
                }
            }
            return Optional.empty();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Erreur lors de la lecture signalement depuis Firestore: " + e.getMessage(), e);
        }
    }


    public List<Map<String, Object>> getSignalementsByEntreprise(Integer idEntreprise) {
        try {
            Firestore db = getFirestore();
            QuerySnapshot querySnapshot = db.collection(COLLECTION_SIGNALEMENTS)
                    .whereEqualTo("idEntreprise", idEntreprise)
                    .get()
                    .get();

            List<Map<String, Object>> signalements = new ArrayList<>();
            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                if (doc.getData() != null) {
                    Map<String, Object> data = doc.getData();

                    if (data.get("dateSuppression") == null) {
                        signalements.add(data);
                    }
                }
            }
            return signalements;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Erreur lors de la recuperation des signalements par entreprise: " + e.getMessage(), e);
        }
    }

    public void deleteSignalement(Integer id) {
        try {
            Firestore db = getFirestore();
            DocumentReference docRef = db.collection(COLLECTION_SIGNALEMENTS).document(String.valueOf(id));

            Map<String, Object> updates = new HashMap<>();
            updates.put("dateSuppression", Instant.now().toString());
            updates.put("dateMiseAJour", Instant.now().toString());

            docRef.update(updates).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Erreur lors de la suppression signalement dans Firestore: " + e.getMessage(), e);
        }
    }

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

    public boolean isAvailable() {
        try {
            Firestore db = getFirestore();
            db.collection("_health_check").document("ping").get().get();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Sauvegarde l'état relatif au blocage (tentatives + bloqueJusqua) pour un utilisateur dans Firestore (merge)
     */
    public void saveUtilisateurState(Integer id, Integer tentativesEchouees, Instant bloqueJusqua) {
        saveUtilisateurState(id, tentativesEchouees, bloqueJusqua, null, null);
    }

    /**
     * Sauvegarde l'etat utilisateur. Cherche d'abord par email si fourni, sinon tente docId == id ou champ 'id'.
     * Ne crée jamais un document avec id "null".
     */
    public void saveUtilisateurState(Integer id, Integer tentativesEchouees, Instant bloqueJusqua, Boolean actif, String email) {
        try {
            Firestore db = getFirestore();

            String docId = null;

            // Priorite: lookup par email si fourni
            if (email != null && !email.isBlank()) {
                Optional<Map<String, Object>> remote = getUtilisateurByEmail(email);
                if (remote.isPresent()) {
                    Object fid = remote.get().get("firebase_id");
                    if (fid instanceof String) docId = (String) fid;
                }
            }

            // Si pas trouve par email, essayer doc avec id == localId (ancienne compat)
            if (docId == null && id != null) {
                DocumentReference refById = db.collection(COLLECTION_USERS).document(String.valueOf(id));
                DocumentSnapshot snap = refById.get().get();
                if (snap.exists()) {
                    docId = snap.getId();
                } else {
                    // fallback: query where field 'id' equals local id
                    QuerySnapshot qs = db.collection(COLLECTION_USERS).whereEqualTo("id", id).limit(1).get().get();
                    if (!qs.isEmpty()) {
                        docId = qs.getDocuments().get(0).getId();
                    }
                }
            }

            if (docId == null) {
                // no document found to update; avoid creating a document with id 'null'
                throw new RuntimeException("Impossible de trouver le document Firestore de l'utilisateur (id et email manquants ou introuvables)");
            }

            DocumentReference docRef = db.collection(COLLECTION_USERS).document(docId);

            Map<String, Object> updates = new HashMap<>();
            updates.put("tentativesEchouees", tentativesEchouees != null ? tentativesEchouees : 0);
            updates.put("bloqueJusqua", bloqueJusqua != null ? bloqueJusqua.toString() : null);
            if (actif != null) updates.put("actif", actif);
            updates.put("dateMiseAJour", Instant.now().toString());
            updates.put("synchronise", true);

            docRef.set(updates, SetOptions.merge()).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Erreur lors de la sauvegarde de l'etat utilisateur dans Firestore: " + e.getMessage(), e);
        }
    }
}
