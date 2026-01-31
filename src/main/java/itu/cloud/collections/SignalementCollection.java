package itu.cloud.collections;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class SignalementCollection extends FirestoreCollection implements Serializable {
     Double budget;
     String dateSuppression;
    String dateCreation;
     String dateMisAJour;
     String description;
    String firebaseUid;
     Integer idEntreprise;
     Location location;
     TypeSignalementDTO idTypeSignalement;
     Double surfaceM2;
    Integer version;
    Integer statut;

    @Getter
    @Setter
    public static class Location {
        Double lat;
        Double lng;
    }

    @Getter
    @Setter
    public static class TypeSignalementDTO {
        String id;
        String nom;
        String icone;
    }
}
