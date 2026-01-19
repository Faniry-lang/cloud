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
public class LoginResponse {
    private boolean success;
    private LoginData data;
    private String error;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginData {
        private String idToken;
        private String refreshToken;
        private String expiresIn;
        private String localId;
        private String email;
        private String displayName;
    }
}

