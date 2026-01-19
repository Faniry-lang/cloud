package itu.cloud.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "signalements")
public class Signalement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "description", length = Integer.MAX_VALUE)
    private String description;

    @Column(name = "surface_m2", precision = 15, scale = 2)
    private BigDecimal surfaceM2;

    @Column(name = "budget", precision = 15, scale = 2)
    private BigDecimal budget;

    @Column(name = "points", columnDefinition = "geometry")
    private Object points;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_entreprise", nullable = false)
    private Entreprise idEntreprise;

    @ColumnDefault("1")
    @Column(name = "version")
    private Integer version;

    @ColumnDefault("now()")
    @Column(name = "date_creation")
    private Instant dateCreation;

    @Column(name = "date_mis_a_jour")
    private Instant dateMisAJour;

    @Column(name = "date_suppression")
    private Instant dateSuppression;


}