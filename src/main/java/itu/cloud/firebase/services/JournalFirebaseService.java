package itu.cloud.firebase.services;

import itu.cloud.collections.JournalCollection;
import org.springframework.stereotype.Service;

@Service
public class JournalFirebaseService extends FirestoreCollectionService<JournalCollection> {
    public JournalFirebaseService(FirebaseService firebaseService) {
        super(firebaseService, "journal", JournalCollection.class);
    }
}
