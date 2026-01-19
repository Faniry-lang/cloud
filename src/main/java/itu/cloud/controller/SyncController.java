package itu.cloud.controller;

import itu.cloud.service.SyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controleur pour la synchronisation des donnees entre PostgreSQL local et Firestore.
 */
@RestController
@RequestMapping("/sync")
@CrossOrigin(origins = "*")
public class SyncController {

    private final SyncService syncService;

    public SyncController(SyncService syncService) {
        this.syncService = syncService;
    }

    /**
     * Synchronise toutes les donnees (bidirectionnel)
     * POST /sync/all
     */
    @PostMapping("/all")
    public ResponseEntity<Map<String, Object>> synchroniserTout() {
        try {
            SyncService.SyncResult result = syncService.synchroniserTout();
            return ResponseEntity.ok(result.toMap());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", "Erreur lors de la synchronisation: " + e.getMessage()
            ));
        }
    }

    /**
     * Synchronise uniquement les utilisateurs vers Firestore
     * POST /sync/utilisateurs
     */
    @PostMapping("/utilisateurs")
    public ResponseEntity<Map<String, Object>> synchroniserUtilisateurs() {
        try {
            SyncService.SyncResult result = syncService.synchroniserUtilisateurs();
            return ResponseEntity.ok(result.toMap());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", "Erreur lors de la synchronisation des utilisateurs: " + e.getMessage()
            ));
        }
    }

    /**
     * Importe les donnees depuis Firestore vers local (pull)
     * POST /sync/pull
     */
    @PostMapping("/pull")
    public ResponseEntity<Map<String, Object>> importerDepuisFirestore() {
        try {
            SyncService.SyncResult result = syncService.synchroniserDepuisFirestore();
            return ResponseEntity.ok(result.toMap());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", "Erreur lors de l'import depuis Firestore: " + e.getMessage()
            ));
        }
    }

    /**
     * Envoie les donnees locales vers Firestore (push)
     * POST /sync/push
     */
    @PostMapping("/push")
    public ResponseEntity<Map<String, Object>> envoyerVersFirestore() {
        try {
            SyncService.SyncResult result = syncService.synchroniserTout();
            return ResponseEntity.ok(result.toMap());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", "Erreur lors de l'envoi vers Firestore: " + e.getMessage()
            ));
        }
    }

    /**
     * Recupere le statut de synchronisation
     * GET /sync/status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatutSynchronisation() {
        try {
            Map<String, Object> status = syncService.getStatutSynchronisation();
            status.put("success", true);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", "Erreur lors de la verification du statut: " + e.getMessage()
            ));
        }
    }
}

