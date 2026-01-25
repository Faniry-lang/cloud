package itu.cloud.controller;

import itu.cloud.dto.ApiResponse;
import itu.cloud.dto.UtilisateurDTO;
import itu.cloud.service.UtilisateurService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/utilisateurs")
@CrossOrigin(origins = "*")
public class UtilisateurController {

    private final UtilisateurService utilisateurService;

    public UtilisateurController(UtilisateurService utilisateurService) {
        this.utilisateurService = utilisateurService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UtilisateurDTO>>> getAll() {
        ApiResponse<List<UtilisateurDTO>> response = utilisateurService.getAll();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UtilisateurDTO>> getById(@PathVariable Integer id) {
        ApiResponse<UtilisateurDTO> response = utilisateurService.getById(id);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(404).body(response);
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<ApiResponse<UtilisateurDTO>> getByEmail(@PathVariable String email) {
        ApiResponse<UtilisateurDTO> response = utilisateurService.getByEmail(email);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(404).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UtilisateurDTO>> update(@PathVariable Integer id, @RequestBody UtilisateurDTO dto) {
        ApiResponse<UtilisateurDTO> response = utilisateurService.update(id, dto);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Integer id) {
        ApiResponse<Void> response = utilisateurService.delete(id);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(404).body(response);
    }
}

