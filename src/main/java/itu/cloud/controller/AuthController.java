package itu.cloud.controller;

import itu.cloud.dto.*;
import itu.cloud.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody RegisterRequest request) {
        try {
            RegisterResponse response = authService.register(request);

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

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);

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

    @PostMapping("/debloquer-utilisateur")
    public ResponseEntity<?> debloquerUtilisateur(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "Email requis"
            ));
        }

        boolean success = authService.debloquerUtilisateur(email);
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
            "configuredMode", authService.getConfiguredMode(),
            "effectiveMode", authService.getAuthMode()
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
            "configuredMode", authService.getConfiguredMode(),
            "effectiveMode", authService.getAuthMode()
        ));
    }

    /**
     * Vérifie si un utilisateur est bloqué
     * GET /auth/statut-blocage/{email}
     */
    @GetMapping("/statut-blocage/{email}")
    public ResponseEntity<?> getStatutBlocage(@PathVariable String email) {
        try {
            Map<String, Object> statut = authService.getStatutBlocage(email);
            return ResponseEntity.ok(statut);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    @PutMapping("/update-user/{email}")
    public ResponseEntity<?> updateUserByEmail(@PathVariable String email, @RequestBody Map<String, Object> updates) {
        try {
            Map<String, Object> result = authService.updateUserByEmail(email, updates);
            if (Boolean.TRUE.equals(result.get("success"))) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

}
