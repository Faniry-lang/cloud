package itu.cloud.firebase.services;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import itu.cloud.collections.UtilisateurCollection;
import itu.cloud.entities.Journal;
import itu.cloud.firebase.utils.FirestoreHelper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class UtilisateurFirebaseService extends FirestoreCollectionService<UtilisateurCollection> {

    private final FirebaseService firebaseService;

    public UtilisateurFirebaseService(FirebaseService firebaseService) {
        super(firebaseService, "utilisateurs", UtilisateurCollection.class);
        this.firebaseService = firebaseService;
    }

    @Override
    public UtilisateurCollection saveFromJournal(Journal journal) throws Exception {
        if ("INSERT".equals(journal.getOperation())) {
            Map<String, Object> donnees = journal.getDonnees();

            Integer id = (Integer) donnees.get("id");
            String email = (String) donnees.get("email");
            String nom = (String) donnees.get("nom");
            String password = (String) donnees.get("password");
            String dateCreation = (String) donnees.get("dateCreation");
            String dateMisAJour = (String) donnees.get("dateMisAJour");
            String role = (String) donnees.get("role");
            Integer tentativesEchouees = 0;

            Map<String, String> firebaseUserData = firebaseService.registerWithFirebase(
                email,
                password,
                nom
            );

            donnees.put("firebaseUid", firebaseUserData.get("localId"));
            donnees.put("email", firebaseUserData.get("email"));
            donnees.put("nom", firebaseUserData.get("displayName"));

            UtilisateurCollection utilisateurCollection = new UtilisateurCollection();
            utilisateurCollection.setId(id);
            utilisateurCollection.setEmail(email);
            utilisateurCollection.setFirebaseUid((String) donnees.get("firebaseUid"));
            utilisateurCollection.setNom(nom);
            utilisateurCollection.setRole(role);
            utilisateurCollection.setTentativesEchouees(tentativesEchouees);
            utilisateurCollection.setDateCreation(dateCreation);
            utilisateurCollection.setDateMiseAJour(dateMisAJour);
            utilisateurCollection.setSynchronise(true);
            utilisateurCollection.setActif(true);

            return this.save(utilisateurCollection);

        } else {
            return super.saveFromJournal(journal);
        }
    }

    public List<UtilisateurCollection> getAllBlockedUsers() throws ExecutionException, InterruptedException {
        List<UtilisateurCollection> allUsers = this.find(null);
        return allUsers.stream()
                .filter(user -> user.getBloqueJusqua() != null && !user.getBloqueJusqua().isEmpty())
                .collect(Collectors.toList());
    }

    public Optional<UtilisateurCollection> findByEmail(String email) throws ExecutionException, InterruptedException {
        Firestore db = firebaseService.getDb();

        QuerySnapshot querySnapshot = db.collection("utilisateurs")
                .whereEqualTo("email", email)
                .get()
                .get();

        if (querySnapshot.isEmpty()) {
            return Optional.empty();
        }

        UtilisateurCollection utilisateur = FirestoreHelper.convert(
                querySnapshot.getDocuments().get(0),
                UtilisateurCollection.class
        );
        utilisateur.setDocId(querySnapshot.getDocuments().get(0).getId());

        return Optional.of(utilisateur);
    }
}
