package itu.cloud.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "statuts_signalement")
public class StatutSignalement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "niveau")
    private Integer niveau;

    @Column(name = "nom", nullable = false, length = 50)
    private String nom;
}