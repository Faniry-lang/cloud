package itu.cloud.controller;

import itu.cloud.dto.ApiResponse;
import itu.cloud.dto.UtilisateurDTO;
import itu.cloud.service.UtilisateurService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controleur CRUD pour les Utilisateurs.
 * Workflow hybride: Firebase par defaut, bascule en local si offline.
 * Note: L'inscription et connexion sont gerees par AuthController.
 */
@RestController
@RequestMapping("/api/utilisateurs")
@CrossOrigin(origins = "*")
public class UtilisateurController {

    private final UtilisateurService utilisateurService;

    public UtilisateurController(UtilisateurService utilisateurService) {
        this.utilisateurService = utilisateurService;
    }

    /**
     * Liste tous les utilisateurs
     * GET /api/utilisateurs
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<UtilisateurDTO>>> getAll() {
        ApiResponse<List<UtilisateurDTO>> response = utilisateurService.getAll();
        return ResponseEntity.ok(response);
    }

    /**
     * Recupere un utilisateur par ID
     * GET /api/utilisateurs/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UtilisateurDTO>> getById(@PathVariable Integer id) {
        ApiResponse<UtilisateurDTO> response = utilisateurService.getById(id);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(404).body(response);
    }

    /**
     * Recupere un utilisateur par email
     * GET /api/utilisateurs/email/{email}
     */
    @GetMapping("/email/{email}")
    public ResponseEntity<ApiResponse<UtilisateurDTO>> getByEmail(@PathVariable String email) {
        ApiResponse<UtilisateurDTO> response = utilisateurService.getByEmail(email);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(404).body(response);
    }

    /**
     * Met a jour un utilisateur
     * PUT /api/utilisateurs/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UtilisateurDTO>> update(@PathVariable Integer id, @RequestBody UtilisateurDTO dto) {
        ApiResponse<UtilisateurDTO> response = utilisateurService.update(id, dto);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Desactive un utilisateur (soft delete)
     * DELETE /api/utilisateurs/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Integer id) {
        ApiResponse<Void> response = utilisateurService.delete(id);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(404).body(response);
    }
}

