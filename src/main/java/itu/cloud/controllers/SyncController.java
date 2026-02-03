package itu.cloud.controllers;

import itu.cloud.service.JournalService;
import itu.cloud.service.SyncService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sync")
public class SyncController {

    private final SyncService syncService;
    private final JournalService journalService;

    public SyncController(SyncService syncService, JournalService journalService) {
        this.syncService = syncService;
        this.journalService = journalService;
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

    @GetMapping("/non-sync-count")
    public ResponseEntity<?> nonSyncCount() {
        try {
            int count = journalService.getJournalNonSync();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "");
            response.put("count", count);

            return ResponseEntity.ok(response);
        } catch(Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors du push: " + e.getMessage()));
        }
    }

    @GetMapping("/all-journal-history")
    public ResponseEntity<?> getAllJournalHistory() {
        try {
            List<JournalService.HistoriqueJournal> historiqueJournals = journalService.getAllJournalHistoryUnsync();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "");
            response.put("histo", historiqueJournals);

            return ResponseEntity.ok(response);
        } catch(Exception e) {
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
