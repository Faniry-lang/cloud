package itu.cloud.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "utilisateurs")
public class Utilisateur {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "nom", length = 100)
    private String nom;

    @Column(name = "mot_de_passe_hash", length = Integer.MAX_VALUE)
    private String motDePasseHash;

    @Column(name = "firebase_uid", length = 128)
    private String firebaseUid;

    @ColumnDefault("0")
    @Column(name = "tentatives_echouees")
    private Integer tentativesEchouees;

    @Column(name = "bloque_jusqua")
    private LocalDateTime bloqueJusqua;

    @ColumnDefault("true")
    @Column(name = "actif")
    private Boolean actif;

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