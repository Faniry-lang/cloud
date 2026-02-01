package itu.cloud.collections;

import com.google.cloud.firestore.annotation.Exclude;

public class FirestoreCollection {
    @Exclude
    String docId;
    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }
}
