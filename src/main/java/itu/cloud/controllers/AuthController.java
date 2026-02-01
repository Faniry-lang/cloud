package itu.cloud.controllers;

import itu.cloud.collections.UtilisateurCollection;
import itu.cloud.dto.AuthDTO;
import itu.cloud.dto.LoginRequest;
import itu.cloud.dto.RegisterRequest;
import itu.cloud.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest registerRequest) {
        try {
            // Validation des données
            if (registerRequest.getData() == null) {
                return ResponseEntity
                        .badRequest()
                        .body(createErrorResponse("Les données d'inscription sont requises"));
            }

            RegisterRequest.RegisterData data = registerRequest.getData();

            // Validation des paramètres
            if (data.getEmail() == null || data.getEmail().isEmpty()) {
                return ResponseEntity
                        .badRequest()
                        .body(createErrorResponse("L'email est requis"));
            }

            if (data.getPassword() == null || data.getPassword().isEmpty()) {
                return ResponseEntity
                        .badRequest()
                        .body(createErrorResponse("Le mot de passe est requis"));
            }

            if (data.getNom() == null || data.getNom().isEmpty()) {
                return ResponseEntity
                        .badRequest()
                        .body(createErrorResponse("Le nom est requis"));
            }

            if (data.getRole() == null || data.getRole().isEmpty()) {
                return ResponseEntity
                        .badRequest()
                        .body(createErrorResponse("Le role est requis"));
            }

            // Inscription
            UtilisateurCollection utilisateur = authService.register(registerRequest);

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(createSuccessResponseForUtilisateur(utilisateur));

        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur serveur: " + e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            if (loginRequest.getData() == null) {
                return ResponseEntity
                        .badRequest()
                        .body(createErrorResponse("Les données de connexion sont requises"));
            }

            LoginRequest.LoginData data = loginRequest.getData();

            if (data.getEmail() == null || data.getEmail().isEmpty()) {
                return ResponseEntity
                        .badRequest()
                        .body(createErrorResponse("L'email est requis"));
            }

            if (data.getPassword() == null || data.getPassword().isEmpty()) {
                return ResponseEntity
                        .badRequest()
                        .body(createErrorResponse("Le mot de passe est requis"));
            }

            AuthDTO authDTO = authService.login(loginRequest);

            return ResponseEntity.ok(createSuccessResponse(authDTO));

        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur serveur: " + e.getMessage()));
        }
    }

    private Map<String, Object> createSuccessResponse(AuthDTO authDTO) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", authDTO);
        return response;
    }

    private Map<String, Object> createSuccessResponseForUtilisateur(UtilisateurCollection utilisateur) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", utilisateur);
        return response;
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        return response;
    }
}
