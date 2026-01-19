package itu.cloud.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "historique_statut_signalement")
public class HistoriqueStatutSignalement {
    @EmbeddedId
    private HistoriqueStatutSignalementId id;

    @MapsId("idSignalement")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @ColumnDefault("nextval('historique_statut_signalement_id_signalement_seq')")
    @JoinColumn(name = "id_signalement", nullable = false)
    private Signalement idSignalement;

    @MapsId("idStatutSignalement")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_statut_signalement", nullable = false)
    private StatutsSignalement idStatutSignalement;

    @ColumnDefault("now()")
    @Column(name = "date_creation")
    private Instant dateCreation;

    @Column(name = "date_mis_a_jour")
    private Instant dateMisAJour;

    @Column(name = "date_suppression")
    private Instant dateSuppression;


}