package itu.cloud.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "historique_statut_signalement")
public class HistoriqueStatutSignalement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_signalement", nullable = false)
    private Signalement idSignalement;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_statut_signalement", nullable = false)
    private StatutSignalement idStatutSignalement;

    @ColumnDefault("now()")
    @Column(name = "date_creation")
    private LocalDateTime dateCreation;


}