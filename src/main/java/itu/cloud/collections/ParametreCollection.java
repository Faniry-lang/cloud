package itu.cloud.collections;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class ParametreCollection extends FirestoreCollection implements Serializable {
    Integer id;
    String type;
    String nom;
    String valeur;
    String dateMisAJour;
}
