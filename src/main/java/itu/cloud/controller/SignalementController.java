package itu.cloud.controller;

import itu.cloud.dto.ApiResponse;
import itu.cloud.dto.SignalementDTO;
import itu.cloud.service.SignalementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/signalements")
@CrossOrigin(origins = "*")
public class SignalementController {

    private final SignalementService signalementService;

    public SignalementController(SignalementService signalementService) {
        this.signalementService = signalementService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SignalementDTO>>> getAll() {
        ApiResponse<List<SignalementDTO>> response = signalementService.getAll();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SignalementDTO>> getById(@PathVariable Integer id) {
        ApiResponse<SignalementDTO> response = signalementService.getById(id);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(404).body(response);
    }


    @GetMapping("/entreprise/{idEntreprise}")
    public ResponseEntity<ApiResponse<List<SignalementDTO>>> getByEntreprise(@PathVariable Integer idEntreprise) {
        ApiResponse<List<SignalementDTO>> response = signalementService.getByEntreprise(idEntreprise);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SignalementDTO>> create(@RequestBody SignalementDTO dto) {
        ApiResponse<SignalementDTO> response = signalementService.create(dto);
        if (response.isSuccess()) {
            return ResponseEntity.status(201).body(response);
        }
        return ResponseEntity.badRequest().body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SignalementDTO>> update(@PathVariable Integer id, @RequestBody SignalementDTO dto) {
        ApiResponse<SignalementDTO> response = signalementService.update(id, dto);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Integer id) {
        ApiResponse<Void> response = signalementService.delete(id);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(404).body(response);
    }
}

