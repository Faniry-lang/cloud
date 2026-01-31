package itu.cloud.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "type_signalement")
public class TypeSignalement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "firebase_uid", length = 100)
    private String firebaseUid;

    @Column(name = "nom", nullable = false, length = 50)
    private String nom;

    @Column(name = "icone", length = 50)
    private String icone;

}