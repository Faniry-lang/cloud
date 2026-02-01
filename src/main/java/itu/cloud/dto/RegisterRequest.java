package itu.cloud.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private RegisterData data;
    private Boolean isOnline;

    @Data
    public static class RegisterData {
        private String email;
        private String password;
        private String nom;
        private String role;
    }
}
