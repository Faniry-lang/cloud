package itu.cloud.firebase.services;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import itu.cloud.collections.FirestoreCollection;
import itu.cloud.firebase.utils.FirestoreHelper;

import java.util.List;
import java.util.concurrent.ExecutionException;

public abstract class FirestoreCollectionService<T extends FirestoreCollection> {
    private FirebaseService firebaseService;
    private String collectionName;
    private Class<T> entityClass;
    
    public FirestoreCollectionService(FirebaseService firebaseService, 
                                      String collectionName, 
                                      Class<T> entityClass) {
        this.firebaseService = firebaseService;
        this.collectionName = collectionName;
        this.entityClass = entityClass;
    }


    public List<T> find(String firebaseUid) throws ExecutionException, InterruptedException {
        Firestore db = firebaseService.getDb();
        Query query;
        
        if (firebaseUid != null && !firebaseUid.isEmpty()) {
            query = db.collection(collectionName).whereEqualTo("id", firebaseUid);
        } else {
            query = db.collection(collectionName);
        }

        ApiFuture<QuerySnapshot> querySnapshotFuture = query.get();
        QuerySnapshot querySnapshot = querySnapshotFuture.get();
        
        return querySnapshot.getDocuments()
                .stream()
                .map(doc -> FirestoreHelper.convert(doc, entityClass))
                .toList();
    }
}
