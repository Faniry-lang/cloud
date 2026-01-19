package itu.cloud.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "statuts_utilisateur")
public class StatutsUtilisateur {
    @EmbeddedId
    private StatutsUtilisateurId id;

    @MapsId("idUtilisateur")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_utilisateur", nullable = false)
    private Utilisateur idUtilisateur;

    @MapsId("idStatut")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_statut", nullable = false)
    private Statut idStatut;

    @ColumnDefault("now()")
    @Column(name = "date_creation")
    private Instant dateCreation;

    @Column(name = "date_mis_a_jour")
    private Instant dateMisAJour;

    @Column(name = "date_suppression")
    private Instant dateSuppression;


}