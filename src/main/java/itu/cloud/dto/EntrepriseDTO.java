package itu.cloud.dto;
import java.time.Instant;

import lombok.NoArgsConstructor;
import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class EntrepriseDTO {
    private Integer version;
    private Instant dateMisAJour;
    private Instant dateCreation;
    private String nom;
    private Integer id;
}





