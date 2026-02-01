package itu.cloud.firebase.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import itu.cloud.collections.FirestoreCollection;
import itu.cloud.entities.Journal;
import itu.cloud.firebase.utils.FirestoreHelper;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public abstract class FirestoreCollectionService<T extends FirestoreCollection> {
    private FirebaseService firebaseService;
    private String collectionName;
    private Class<T> entityClass;
    private ObjectMapper objectMapper;

    public FirestoreCollectionService(FirebaseService firebaseService, 
                                      String collectionName, 
                                      Class<T> entityClass) {
        this.firebaseService = firebaseService;
        this.collectionName = collectionName;
        this.entityClass = entityClass;
        this.objectMapper = new ObjectMapper();
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

    public T saveFromJournal(Journal journal) throws Exception {
        Firestore db = this.firebaseService.getDb();

        if ("INSERT".equals(journal.getOperation())) {
            T entity = objectMapper.convertValue(journal.getDonnees(), entityClass);

            CollectionReference colRef = db.collection(collectionName);
            ApiFuture<DocumentReference> future = colRef.add(entity);
            DocumentReference docRef = future.get();
            entity.setDocId(docRef.getId());

            return entity;

        } else if ("UPDATE".equals(journal.getOperation())) {
            String documentId = journal.getIdEntite();

            DocumentReference docRef = db.collection(collectionName).document(documentId);
            DocumentSnapshot snapshot = docRef.get().get();

            if (!snapshot.exists()) {
                throw new RuntimeException(
                    "Document with id " + documentId + " does not exist in collection " + collectionName
                );
            }

            T entity = FirestoreHelper.convert(snapshot, entityClass);
            entity.setDocId(documentId);

            Map<String, Object> donnees = journal.getDonnees();
            for (Map.Entry<String, Object> entry : donnees.entrySet()) {
                if ("docId".equals(entry.getKey())) {
                    continue;
                }

                try {
                    Field field = entityClass.getDeclaredField(entry.getKey());
                    field.setAccessible(true);

                    Object value = entry.getValue();
                    Class<?> fieldType = field.getType();

                    if (value != null && !fieldType.isAssignableFrom(value.getClass())) {

                        if (fieldType == Double.class || fieldType == double.class) {
                            if (value instanceof Number) {
                                value = ((Number) value).doubleValue();
                            }
                        } else if (fieldType == Float.class || fieldType == float.class) {
                            if (value instanceof Number) {
                                value = ((Number) value).floatValue();
                            }
                        } else if (fieldType == Long.class || fieldType == long.class) {
                            if (value instanceof Number) {
                                value = ((Number) value).longValue();
                            }
                        } else if (fieldType == Integer.class || fieldType == int.class) {
                            if (value instanceof Number) {
                                value = ((Number) value).intValue();
                            }
                        } else if (fieldType == Boolean.class || fieldType == boolean.class) {
                            if (value instanceof String) {
                                value = Boolean.parseBoolean((String) value);
                            }
                        }
                        else if (value instanceof Map) {
                            value = objectMapper.convertValue(value, fieldType);
                        }
                    }

                    field.set(entity, value);
                } catch (NoSuchFieldException e) {
                    if (!"docId".equals(entry.getKey())) {
                        System.out.println("Field " + entry.getKey() + " not found in " + entityClass.getName());
                    }
                } catch (IllegalAccessException e) {
                    System.out.println("Cannot access field " + entry.getKey() + " in " + entityClass.getName());
                } catch (Exception e) {
                    System.out.println("Error setting field " + entry.getKey() + " in " + entityClass.getName() + ": " + e.getMessage());
                }
            }

            docRef.set(entity).get();

            return entity;

        } else {
            throw new UnsupportedOperationException("Operation " + journal.getOperation() + " not supported");
        }
    }

}

