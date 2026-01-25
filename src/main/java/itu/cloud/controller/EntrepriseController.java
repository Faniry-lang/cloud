package itu.cloud.controller;

import itu.cloud.dto.ApiResponse;
import itu.cloud.dto.EntrepriseDTO;
import itu.cloud.service.EntrepriseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/entreprises")
@CrossOrigin(origins = "*")
public class EntrepriseController {

    private final EntrepriseService entrepriseService;

    public EntrepriseController(EntrepriseService entrepriseService) {
        this.entrepriseService = entrepriseService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<EntrepriseDTO>>> getAll() {
        ApiResponse<List<EntrepriseDTO>> response = entrepriseService.getAll();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EntrepriseDTO>> getById(@PathVariable Integer id) {
        ApiResponse<EntrepriseDTO> response = entrepriseService.getById(id);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(404).body(response);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EntrepriseDTO>> create(@RequestBody EntrepriseDTO dto) {
        ApiResponse<EntrepriseDTO> response = entrepriseService.create(dto);
        if (response.isSuccess()) {
            return ResponseEntity.status(201).body(response);
        }
        return ResponseEntity.badRequest().body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EntrepriseDTO>> update(@PathVariable Integer id, @RequestBody EntrepriseDTO dto) {
        ApiResponse<EntrepriseDTO> response = entrepriseService.update(id, dto);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Integer id) {
        ApiResponse<Void> response = entrepriseService.delete(id);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(404).body(response);
    }
}

