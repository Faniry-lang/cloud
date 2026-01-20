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
public class UtilisateurDTO {
    private Integer id;
    private String email;
    private String nom;
    private String firebaseUid;
    private Boolean actif;
    private Integer version;
    private String role;
    private String statut;
    private Instant dateCreation;
    private Instant dateMisAJour;
}

