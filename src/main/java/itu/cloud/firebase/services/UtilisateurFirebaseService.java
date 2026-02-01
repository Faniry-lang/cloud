package itu.cloud.firebase.services;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import itu.cloud.collections.UtilisateurCollection;
import itu.cloud.firebase.utils.FirestoreHelper;
import org.springframework.stereotype.Service;

import java.util.List;
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
