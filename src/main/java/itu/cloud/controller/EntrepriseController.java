package itu.cloud.controller;

import itu.cloud.dto.ApiResponse;
import itu.cloud.dto.EntrepriseDTO;
import itu.cloud.service.EntrepriseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controleur CRUD pour les Entreprises.
 * Workflow hybride: Firebase par defaut, bascule en local si offline.
 */
@RestController
@RequestMapping("/api/entreprises")
@CrossOrigin(origins = "*")
public class EntrepriseController {

    private final EntrepriseService entrepriseService;

    public EntrepriseController(EntrepriseService entrepriseService) {
        this.entrepriseService = entrepriseService;
    }

    /**
     * Liste toutes les entreprises
     * GET /api/entreprises
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<EntrepriseDTO>>> getAll() {
        ApiResponse<List<EntrepriseDTO>> response = entrepriseService.getAll();
        return ResponseEntity.ok(response);
    }

    /**
     * Recupere une entreprise par ID
     * GET /api/entreprises/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EntrepriseDTO>> getById(@PathVariable Integer id) {
        ApiResponse<EntrepriseDTO> response = entrepriseService.getById(id);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(404).body(response);
    }

    /**
     * Cree une nouvelle entreprise
     * POST /api/entreprises
     */
    @PostMapping
    public ResponseEntity<ApiResponse<EntrepriseDTO>> create(@RequestBody EntrepriseDTO dto) {
        ApiResponse<EntrepriseDTO> response = entrepriseService.create(dto);
        if (response.isSuccess()) {
            return ResponseEntity.status(201).body(response);
        }
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Met a jour une entreprise
     * PUT /api/entreprises/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EntrepriseDTO>> update(@PathVariable Integer id, @RequestBody EntrepriseDTO dto) {
        ApiResponse<EntrepriseDTO> response = entrepriseService.update(id, dto);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Supprime une entreprise (soft delete)
     * DELETE /api/entreprises/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Integer id) {
        ApiResponse<Void> response = entrepriseService.delete(id);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(404).body(response);
    }
}

