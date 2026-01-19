package itu.cloud.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {
    private boolean success;
    private AuthData data;
    private String error;
    private String authMode; // LOCAL ou FIREBASE

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthData {
        // Données communes
        private Integer userId;
        private String email;
        private String nom;
        private String role;
        private String statut;

        // Token local (JWT)
        private String token;
        private Long expiresIn;

        // Données Firebase (si mode FIREBASE ou HYBRID)
        private String idToken;
        private String refreshToken;
        private String firebaseUid;
    }
}

