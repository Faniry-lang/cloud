package itu.cloud.collections;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class UtilisateurCollection extends FirestoreCollection implements Serializable {
    String firebaseUid;
    String dateCreation;
    String dateMiseAJour;
    String email;
    Integer id;
    String nom;
    String role;
    boolean synchronise;
    Integer version;
    String bloqueJusqua;
    Integer tentativesEchouees;
    boolean actif;
}
