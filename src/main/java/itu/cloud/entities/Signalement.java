package itu.cloud.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "signalements")
public class Signalement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "firebase_uid", length = 100)
    private String firebaseUid;

    @Column(name = "description", length = Integer.MAX_VALUE)
    private String description;

    @Column(name = "surface_m2")
    private double surfaceM2;

    @Column(name = "budget")
    private double budget;

    @Column(name = "points", columnDefinition = "geometry")
    private Object points;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_entreprise")
    private Entreprise idEntreprise;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_type_signalement")
    private TypeSignalement idTypeSignalement;

    @ColumnDefault("1")
    @Column(name = "version")
    private Integer version;

    @ColumnDefault("now()")
    @Column(name = "date_creation")
    private LocalDateTime dateCreation;

    @Column(name = "date_mis_a_jour")
    private LocalDateTime dateMisAJour;

    @Column(name = "date_suppression")
    private LocalDateTime dateSuppression;


}