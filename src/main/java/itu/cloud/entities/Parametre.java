package itu.cloud.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "parametres")
public class Parametre {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "nom", nullable = false, length = 50)
    private String nom;

    @Column(name = "valeur", nullable = false)
    private String valeur;

    @Column(name = "type", length = 50)
    private String type;

    @ColumnDefault("now()")
    @Column(name = "date_creation")
    private LocalDateTime dateCreation;

    @Column(name = "date_mis_a_jour")
    private LocalDateTime dateMisAJour;


}