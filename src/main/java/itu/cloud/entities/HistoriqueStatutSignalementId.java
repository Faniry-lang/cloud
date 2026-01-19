package itu.cloud.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.io.Serializable;

@Getter
@Setter
@EqualsAndHashCode
@Embeddable
public class HistoriqueStatutSignalementId implements Serializable {
    private static final long serialVersionUID = -2222509543675627530L;
    @ColumnDefault("nextval('historique_statut_signalement_id_signalement_seq')")
    @Column(name = "id_signalement", nullable = false)
    private Integer idSignalement;

    @Column(name = "id_statut_signalement", nullable = false)
    private Integer idStatutSignalement;


}