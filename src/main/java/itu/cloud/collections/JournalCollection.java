package itu.cloud.collections;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Map;

@Getter
@Setter
public class JournalCollection extends FirestoreCollection implements Serializable {
    String operation;
    String idEntite;
    String typeEntite;
    Integer version;
    boolean synchronise;
    String localId;
    String dateCreation;
    Map<String, Object> donnees;
}
