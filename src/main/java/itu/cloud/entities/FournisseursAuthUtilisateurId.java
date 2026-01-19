package itu.cloud.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@EqualsAndHashCode
@Embeddable
public class FournisseursAuthUtilisateurId implements Serializable {
    private static final long serialVersionUID = 225457526933773225L;
    @Column(name = "id_utilisateur", nullable = false)
    private Integer idUtilisateur;

    @Column(name = "id_fournisseur_auth", nullable = false)
    private Integer idFournisseurAuth;


}