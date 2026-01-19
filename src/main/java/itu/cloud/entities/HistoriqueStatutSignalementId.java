package itu.cloud.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@EqualsAndHashCode
@Embeddable
public class HistoriqueStatutSignalementId implements Serializable {
    private static final long serialVersionUID = 7061133776568303804L;
    @Column(name = "id_signalement", nullable = false)
    private UUID idSignalement;

    @Column(name = "id_statut_signalement", nullable = false)
    private Integer idStatutSignalement;


}