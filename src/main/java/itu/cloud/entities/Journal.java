package itu.cloud.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "journal")
public class Journal {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "id_entite")
    private UUID idEntite;

    @Column(name = "type_entite", length = Integer.MAX_VALUE)
    private String typeEntite;

    @Column(name = "operation", length = Integer.MAX_VALUE)
    private String operation;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "donnees")
    private Map<String, Object> donnees;

    @Column(name = "version")
    private Integer version;

    @ColumnDefault("now()")
    @Column(name = "date_creation")
    private Instant dateCreation;

    @ColumnDefault("false")
    @Column(name = "synchronise")
    private Boolean synchronise;


}