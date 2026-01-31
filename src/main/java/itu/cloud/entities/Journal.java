package itu.cloud.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "journal")
public class Journal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "id_entite")
    private Integer idEntite;

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
    private LocalDateTime dateCreation;

    @ColumnDefault("false")
    @Column(name = "synchronise")
    private Boolean synchronise;


}