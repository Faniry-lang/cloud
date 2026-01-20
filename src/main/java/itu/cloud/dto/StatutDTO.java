package itu.cloud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatutDTO {
    private Integer id;
    private String description;
    private Instant dateCreation;
    private Instant dateMisAJour;
}

