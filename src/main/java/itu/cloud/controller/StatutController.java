package itu.cloud.controller;

import itu.cloud.dto.ApiResponse;
import itu.cloud.dto.StatutDTO;
import itu.cloud.service.StatutService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/statuts")
@CrossOrigin(origins = "*")
public class StatutController {

    private final StatutService statutService;

    public StatutController(StatutService statutService) {
        this.statutService = statutService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<StatutDTO>>> getAll() {
        ApiResponse<List<StatutDTO>> response = statutService.getAll();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StatutDTO>> getById(@PathVariable Integer id) {
        ApiResponse<StatutDTO> response = statutService.getById(id);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(404).body(response);
    }


    @PostMapping
    public ResponseEntity<ApiResponse<StatutDTO>> create(@RequestBody StatutDTO dto) {
        ApiResponse<StatutDTO> response = statutService.create(dto);
        if (response.isSuccess()) {
            return ResponseEntity.status(201).body(response);
        }
        return ResponseEntity.badRequest().body(response);
    }


    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StatutDTO>> update(@PathVariable Integer id, @RequestBody StatutDTO dto) {
        ApiResponse<StatutDTO> response = statutService.update(id, dto);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Integer id) {
        ApiResponse<Void> response = statutService.delete(id);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(404).body(response);
    }
}

