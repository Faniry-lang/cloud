package itu.cloud.controllers;

import itu.cloud.collections.UtilisateurCollection;
import itu.cloud.firebase.services.UtilisateurFirebaseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/manager/user")
public class UtilisateurController {

    private final UtilisateurFirebaseService utilisateurFirebaseService;

    public UtilisateurController(UtilisateurFirebaseService utilisateurFirebaseService) {
        this.utilisateurFirebaseService = utilisateurFirebaseService;
    }

    @GetMapping("/all")
    public List<UtilisateurCollection> getAllUtilisateurs() throws ExecutionException, InterruptedException {
        return this.utilisateurFirebaseService.find(null);
    }

    @GetMapping("/blocked")
    public ResponseEntity<?> getAllBlockedUsers() {
        try {
            List<UtilisateurCollection> blockedUsers = utilisateurFirebaseService.getAllBlockedUsers();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", blockedUsers.size());
            response.put("data", blockedUsers);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors de la récupération des utilisateurs bloqués: " + e.getMessage()));
        }
    }

    @PostMapping("/unblock/{documentId}")
    public ResponseEntity<?> unblockUser(@PathVariable String documentId) {
        try {
            if (documentId == null || documentId.isEmpty()) {
                return ResponseEntity
                        .badRequest()
                        .body(createErrorResponse("L'ID du document est requis"));
            }

            List<UtilisateurCollection> utilisateurs = utilisateurFirebaseService.find(documentId);

            if (utilisateurs == null || utilisateurs.isEmpty()) {
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("Utilisateur non trouvé"));
            }

            UtilisateurCollection utilisateur = utilisateurs.get(0);

            utilisateur.setTentativesEchouees(0);
            utilisateur.setBloqueJusqua(null);
            utilisateur.setActif(true);

            UtilisateurCollection updatedUser = utilisateurFirebaseService.update(documentId, utilisateur);

            return ResponseEntity.ok(createSuccessResponse(updatedUser, "Utilisateur débloqué avec succès"));

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors du déblocage: " + e.getMessage()));
        }
    }

    private Map<String, Object> createSuccessResponse(UtilisateurCollection utilisateur, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
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
