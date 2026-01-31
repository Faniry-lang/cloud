package itu.cloud.firebase.services;

import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;
@Service
public class FirebaseService {
    public Firestore getDb() {
        return FirestoreClient.getFirestore();
    }
}
