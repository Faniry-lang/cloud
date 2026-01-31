package itu.cloud.firebase.services;

import itu.cloud.collections.SignalementCollection;
import org.springframework.stereotype.Service;

@Service
public class SignalementFirebaseService extends FirestoreCollectionService<SignalementCollection> {

    public SignalementFirebaseService(FirebaseService firebaseService) {
        super(firebaseService, "signalements", SignalementCollection.class);
    }
}
