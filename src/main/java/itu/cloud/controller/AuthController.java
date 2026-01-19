package itu.cloud.controller;

import itu.cloud.dto.*;
import itu.cloud.service.HybridAuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final HybridAuthService hybridAuthService;

    public AuthController(HybridAuthService hybridAuthService) {
        this.hybridAuthService = hybridAuthService;
    }

    /**
     * Inscription d'un nouvel utilisateur
     * POST /auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody RegisterRequest request) {
        try {
            RegisterResponse response = hybridAuthService.register(request);

            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                RegisterResponse.builder()
                    .success(false)
                    .error("Erreur lors de l'inscription: " + e.getMessage())
                    .build()
            );
        }
    }

    /**
     * Connexion utilisateur (hybride: local + Firebase)
     * POST /auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        try {
            AuthResponse response = hybridAuthService.login(request);

            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(401).body(response);
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                AuthResponse.builder()
                    .success(false)
                    .error("Erreur lors de la connexion: " + e.getMessage())
                    .build()
            );
        }
    }

    /**
     * Debloquer un utilisateur (endpoint admin)
     * POST /auth/debloquer-utilisateur
     */
    @PostMapping("/debloquer-utilisateur")
    public ResponseEntity<?> debloquerUtilisateur(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "Email requis"
            ));
        }

        boolean success = hybridAuthService.debloquerUtilisateur(email);
        if (success) {
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Utilisateur debloque avec succes"
            ));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "Utilisateur non trouve"
            ));
        }
    }

    /**
     * Retourne le mode d'authentification actuel
     * GET /auth/mode
     */
    @GetMapping("/mode")
    public ResponseEntity<?> getAuthMode() {
        return ResponseEntity.ok(Map.of(
            "configuredMode", hybridAuthService.getConfiguredMode(),
            "effectiveMode", hybridAuthService.getAuthMode()
        ));
    }

    /**
     * Health check pour l'API d'authentification
     * GET /auth/health
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "Authentication API",
            "configuredMode", hybridAuthService.getConfiguredMode(),
            "effectiveMode", hybridAuthService.getAuthMode()
        ));
    }
}

