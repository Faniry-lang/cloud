package itu.cloud.controller;

import itu.cloud.dto.ApiResponse;
import itu.cloud.dto.SignalementDTO;
import itu.cloud.service.SignalementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controleur CRUD pour les Signalements.
 * Workflow hybride: Firebase par defaut, bascule en local si offline.
 */
@RestController
@RequestMapping("/api/signalements")
@CrossOrigin(origins = "*")
public class SignalementController {

    private final SignalementService signalementService;

    public SignalementController(SignalementService signalementService) {
        this.signalementService = signalementService;
    }

    /**
     * Liste tous les signalements
     * GET /api/signalements
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<SignalementDTO>>> getAll() {
        ApiResponse<List<SignalementDTO>> response = signalementService.getAll();
        return ResponseEntity.ok(response);
    }

    /**
     * Recupere un signalement par ID
     * GET /api/signalements/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SignalementDTO>> getById(@PathVariable Integer id) {
        ApiResponse<SignalementDTO> response = signalementService.getById(id);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(404).body(response);
    }

    /**
     * Liste les signalements par entreprise
     * GET /api/signalements/entreprise/{idEntreprise}
     */
    @GetMapping("/entreprise/{idEntreprise}")
    public ResponseEntity<ApiResponse<List<SignalementDTO>>> getByEntreprise(@PathVariable Integer idEntreprise) {
        ApiResponse<List<SignalementDTO>> response = signalementService.getByEntreprise(idEntreprise);
        return ResponseEntity.ok(response);
    }

    /**
     * Cree un nouveau signalement
     * POST /api/signalements
     */
    @PostMapping
    public ResponseEntity<ApiResponse<SignalementDTO>> create(@RequestBody SignalementDTO dto) {
        ApiResponse<SignalementDTO> response = signalementService.create(dto);
        if (response.isSuccess()) {
            return ResponseEntity.status(201).body(response);
        }
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Met a jour un signalement
     * PUT /api/signalements/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SignalementDTO>> update(@PathVariable Integer id, @RequestBody SignalementDTO dto) {
        ApiResponse<SignalementDTO> response = signalementService.update(id, dto);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Supprime un signalement (soft delete)
     * DELETE /api/signalements/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Integer id) {
        ApiResponse<Void> response = signalementService.delete(id);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(404).body(response);
    }
}

