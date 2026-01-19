package itu.cloud.service;

import itu.cloud.dto.LoginRequest;
import itu.cloud.dto.LoginResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class FirebaseAuthService {

    @Value("${firebase.apiKey}")
    private String firebaseApiKey;

    private static final String FIREBASE_AUTH_URL = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=";

    private final RestTemplate restTemplate;

    public FirebaseAuthService() {
        this.restTemplate = new RestTemplate();
    }

    @SuppressWarnings("unchecked")
    public LoginResponse login(LoginRequest request) {
        if (request.getEmail() == null || request.getEmail().isEmpty() ||
            request.getPassword() == null || request.getPassword().isEmpty()) {
            return LoginResponse.builder()
                    .success(false)
                    .error("Email et mot de passe requis")
                    .build();
        }

        try {
            String url = FIREBASE_AUTH_URL + firebaseApiKey;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("email", request.getEmail());
            body.put("password", request.getPassword());
            body.put("returnSecureToken", true);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();

                LoginResponse.LoginData data = LoginResponse.LoginData.builder()
                        .idToken((String) responseBody.get("idToken"))
                        .refreshToken((String) responseBody.get("refreshToken"))
                        .expiresIn((String) responseBody.get("expiresIn"))
                        .localId((String) responseBody.get("localId"))
                        .email((String) responseBody.get("email"))
                        .displayName((String) responseBody.get("displayName"))
                        .build();

                return LoginResponse.builder()
                        .success(true)
                        .data(data)
                        .build();
            }

            return LoginResponse.builder()
                    .success(false)
                    .error("Réponse invalide de Firebase")
                    .build();

        } catch (HttpClientErrorException e) {
            String errorMessage = extractFirebaseError(e);
            String userMessage = translateFirebaseError(errorMessage);

            return LoginResponse.builder()
                    .success(false)
                    .error(userMessage)
                    .build();
        } catch (Exception e) {
            return LoginResponse.builder()
                    .success(false)
                    .error("Une erreur est survenue lors de la connexion: " + e.getMessage())
                    .build();
        }
    }

    private String extractFirebaseError(HttpClientErrorException e) {
        try {
            String responseBody = e.getResponseBodyAsString();
            if (responseBody.contains("\"message\"")) {
                int start = responseBody.indexOf("\"message\"") + 11;
                int end = responseBody.indexOf("\"", start);
                return responseBody.substring(start, end);
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return "UNKNOWN_ERROR";
    }

    private String translateFirebaseError(String errorMessage) {
        return switch (errorMessage) {
            case "EMAIL_NOT_FOUND" -> "Aucun compte trouvé avec cet email";
            case "INVALID_PASSWORD" -> "Mot de passe incorrect";
            case "USER_DISABLED" -> "Ce compte a été désactivé";
            case "INVALID_LOGIN_CREDENTIALS" -> "Email ou mot de passe incorrect";
            case "TOO_MANY_ATTEMPTS_TRY_LATER" -> "Trop de tentatives. Veuillez réessayer plus tard";
            default -> errorMessage;
        };
    }
}
