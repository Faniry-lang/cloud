package itu.cloud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignalementDTO {
    private Integer id;
    private String description;
    private BigDecimal surfaceM2;
    private BigDecimal budget;
    private Integer idEntreprise;
    private String entrepriseNom;
    private Integer version;
    private Instant dateCreation;
    private Instant dateMisAJour;
}

