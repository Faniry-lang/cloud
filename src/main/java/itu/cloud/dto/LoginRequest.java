package itu.cloud.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private LoginData data;
    private Boolean isOnline;

    @Data
    public static class LoginData {
        private String email;
        private String password;
    }
}
