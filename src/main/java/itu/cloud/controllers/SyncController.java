package itu.cloud.controllers;

import itu.cloud.service.SyncService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/sync")
public class SyncController {

    private final SyncService syncService;

    public SyncController(SyncService syncService) {
        this.syncService = syncService;
    }

    @PostMapping("/pull")
    public ResponseEntity<?> pull() {
        try {
            int count =
                    syncService.pull();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Pull effectué avec succès");
            response.put("signalementsSynchronises", count);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.out.println(e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors du pull: " + e.getMessage()));
        }
    }

    @PostMapping("/push")
    public ResponseEntity<?> push() {
        try {
            int count = syncService.push();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Push effectué avec succès");
            response.put("journauxSynchronises", count);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors du push: " + e.getMessage()));
        }
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        return response;
    }
}
