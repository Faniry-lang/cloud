package itu.cloud.firebase.services;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import itu.cloud.collections.FirestoreCollection;
import itu.cloud.firebase.utils.FirestoreHelper;

import java.util.ArrayList;
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

    public List<T> find(String documentId)
            throws ExecutionException, InterruptedException {

        Firestore db = firebaseService.getDb();
        List<T> collection = new ArrayList<>();

        if (documentId != null && !documentId.isEmpty()) {

            DocumentSnapshot doc = db
                    .collection(collectionName)
                    .document(documentId)
                    .get()
                    .get();

            if (!doc.exists()) {
                return collection;
            }

            T entity = FirestoreHelper.convert(
                    doc,
                    entityClass
            );
            entity.setDocId(doc.getId());
            collection.add(entity);

            return collection;
        }

        ApiFuture<QuerySnapshot> future =
                db.collection(collectionName).get();

        QuerySnapshot querySnapshot = future.get();

        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
            T entity = FirestoreHelper.convert(
                    doc,
                    entityClass
            );
            entity.setDocId(doc.getId());
            collection.add(entity);
        }

        return collection;
    }


    public T update(String documentId, T document) throws Exception {
        Firestore db = this.firebaseService.getDb();

        DocumentReference docRef = db
                .collection(collectionName)
                .document(documentId);

        DocumentSnapshot snapshot = docRef.get().get();
        if (!snapshot.exists()) {
            throw new RuntimeException(
                    "Document with id " + documentId + " does not exist in collection " + collectionName
            );
        }

        docRef.set(document).get();

        return document;
    }

    public T save(T document) throws Exception {
        Firestore db = this.firebaseService.getDb();
        CollectionReference colRef = db.collection(collectionName);
        ApiFuture<DocumentReference> future = colRef.add(document);
        DocumentReference docRef = future.get();
        document.setDocId(docRef.getId());
        return document;
    }

}
