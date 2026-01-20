package itu.cloud.controller;

import itu.cloud.dto.ApiResponse;
import itu.cloud.dto.StatutDTO;
import itu.cloud.service.StatutService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controleur CRUD pour les Statuts.
 * Workflow hybride: Firebase par defaut, bascule en local si offline.
 */
@RestController
@RequestMapping("/api/statuts")
@CrossOrigin(origins = "*")
public class StatutController {

    private final StatutService statutService;

    public StatutController(StatutService statutService) {
        this.statutService = statutService;
    }

    /**
     * Liste tous les statuts
     * GET /api/statuts
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<StatutDTO>>> getAll() {
        ApiResponse<List<StatutDTO>> response = statutService.getAll();
        return ResponseEntity.ok(response);
    }

    /**
     * Recupere un statut par ID
     * GET /api/statuts/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StatutDTO>> getById(@PathVariable Integer id) {
        ApiResponse<StatutDTO> response = statutService.getById(id);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(404).body(response);
    }

    /**
     * Cree un nouveau statut
     * POST /api/statuts
     */
    @PostMapping
    public ResponseEntity<ApiResponse<StatutDTO>> create(@RequestBody StatutDTO dto) {
        ApiResponse<StatutDTO> response = statutService.create(dto);
        if (response.isSuccess()) {
            return ResponseEntity.status(201).body(response);
        }
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Met a jour un statut
     * PUT /api/statuts/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StatutDTO>> update(@PathVariable Integer id, @RequestBody StatutDTO dto) {
        ApiResponse<StatutDTO> response = statutService.update(id, dto);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Supprime un statut (soft delete)
     * DELETE /api/statuts/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Integer id) {
        ApiResponse<Void> response = statutService.delete(id);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(404).body(response);
    }
}

