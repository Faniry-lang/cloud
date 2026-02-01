package itu.cloud.firebase.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
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
public class FirebaseService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${firebase.apiKey}")
    private String firebaseApiKey;

    @Value("${firebase.authUrl}")
    private String firebaseAuthUrl;

    @Value("${firebase.registerUrl}")
    private String firebaseRegisterUrl;

    public FirebaseService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public Firestore getDb() {
        return FirestoreClient.getFirestore();
    }

    public Map<String, String> registerWithFirebase(String email, String password, String displayName) {
        try {
            String url = firebaseRegisterUrl + firebaseApiKey;

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("email", email);
            requestBody.put("password", password);
            requestBody.put("returnSecureToken", true);
            if (displayName != null && !displayName.isEmpty()) {
                requestBody.put("displayName", displayName);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            JsonNode jsonNode = objectMapper.readTree(response.getBody());

            Map<String, String> userData = new HashMap<>();
            userData.put("email", jsonNode.get("email").asText());
            userData.put("displayName", displayName != null ? displayName : email);
            userData.put("localId", jsonNode.get("localId").asText());

            return userData;

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'inscription Firebase: " + e.getMessage(), e);
        }
    }

    public Map<String, String> authenticateWithFirebase(String email, String password) {
        try {
            String url = firebaseAuthUrl + firebaseApiKey;
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("email", email);
            requestBody.put("password", password);
            requestBody.put("returnSecureToken", true);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            JsonNode jsonNode = objectMapper.readTree(response.getBody());

            Map<String, String> userData = new HashMap<>();
            userData.put("email", jsonNode.get("email").asText());
            userData.put("displayName", jsonNode.has("displayName") ? jsonNode.get("displayName").asText() : email);
            userData.put("localId", jsonNode.get("localId").asText());

            return userData;

        } catch (HttpClientErrorException e) {

            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Erreur d'authentification Firebase: " + e.getMessage(), e);
        }
    }
}
