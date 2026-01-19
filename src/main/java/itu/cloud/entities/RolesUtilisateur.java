package itu.cloud.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "roles_utilisateur")
public class RolesUtilisateur {
    @EmbeddedId
    private RolesUtilisateurId id;

    @MapsId("idUtilisateur")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_utilisateur", nullable = false)
    private Utilisateur idUtilisateur;

    @MapsId("idRole")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_role", nullable = false)
    private Role idRole;

    @ColumnDefault("now()")
    @Column(name = "date_creation")
    private Instant dateCreation;

    @Column(name = "date_mis_a_jour")
    private Instant dateMisAJour;

    @Column(name = "date_suppression")
    private Instant dateSuppression;


}