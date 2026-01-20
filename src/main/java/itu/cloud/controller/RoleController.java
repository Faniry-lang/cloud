package itu.cloud.controller;

import itu.cloud.dto.ApiResponse;
import itu.cloud.dto.RoleDTO;
import itu.cloud.service.RoleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controleur CRUD pour les Roles.
 * Workflow hybride: Firebase par defaut, bascule en local si offline.
 */
@RestController
@RequestMapping("/api/roles")
@CrossOrigin(origins = "*")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    /**
     * Liste tous les roles
     * GET /api/roles
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<RoleDTO>>> getAll() {
        ApiResponse<List<RoleDTO>> response = roleService.getAll();
        return ResponseEntity.ok(response);
    }

    /**
     * Recupere un role par ID
     * GET /api/roles/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleDTO>> getById(@PathVariable Integer id) {
        ApiResponse<RoleDTO> response = roleService.getById(id);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(404).body(response);
    }

    /**
     * Cree un nouveau role
     * POST /api/roles
     */
    @PostMapping
    public ResponseEntity<ApiResponse<RoleDTO>> create(@RequestBody RoleDTO dto) {
        ApiResponse<RoleDTO> response = roleService.create(dto);
        if (response.isSuccess()) {
            return ResponseEntity.status(201).body(response);
        }
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Met a jour un role
     * PUT /api/roles/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleDTO>> update(@PathVariable Integer id, @RequestBody RoleDTO dto) {
        ApiResponse<RoleDTO> response = roleService.update(id, dto);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Supprime un role (soft delete)
     * DELETE /api/roles/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Integer id) {
        ApiResponse<Void> response = roleService.delete(id);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(404).body(response);
    }
}

