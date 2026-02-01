package itu.cloud.collections;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class TypeSignalementCollection extends FirestoreCollection implements Serializable {
    Integer postgres_id;
    String nom;
    String icone;
}
