package itu.cloud.firebase.services;

import itu.cloud.collections.TypeSignalementCollection;
import org.springframework.stereotype.Service;

@Service
public class TypeSignalementFirebaseService extends FirestoreCollectionService<TypeSignalementCollection> {

    public TypeSignalementFirebaseService(FirebaseService firebaseService) {
        super(firebaseService, "typeSignalement", TypeSignalementCollection.class);
    }
}
