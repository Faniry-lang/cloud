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
    public ResponseEntity<ApiResponse<SignalementDTO>> getById(@PathVariable String id) {
        String postgresId = id.split("-")[0];
        String firebaseId = id.split("-")[1];
        ApiResponse<SignalementDTO> responseFromFirestore = signalementService.getByIdFromFirebase(firebaseId);
        if (responseFromFirestore.isSuccess()) {
            return ResponseEntity.ok(responseFromFirestore);
        } else if(postgresId != null && !postgresId.isEmpty()) {
            ApiResponse<SignalementDTO> responseFromLocalPg = signalementService.getById(Integer.valueOf(postgresId));
            if(responseFromLocalPg.isSuccess()) {
                return ResponseEntity.ok(responseFromLocalPg);
            }

        }
        return ResponseEntity.status(404).body(responseFromFirestore);
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
    public ResponseEntity<ApiResponse<SignalementDTO>> update(@PathVariable String id, @RequestBody SignalementDTO dto) {
        String postgresId = id.split("-")[0];
        String firebaseId = id.split("-")[1];
        ApiResponse<SignalementDTO> responseFromFirestore = signalementService.updateToFirebase(firebaseId, dto);
        if (responseFromFirestore.isSuccess()) {
            return ResponseEntity.ok(responseFromFirestore);
        } else if(postgresId != null && !postgresId.isEmpty()) {
            ApiResponse<SignalementDTO> responseFromLocalPg = signalementService.update(Integer.valueOf(postgresId), dto);
            if(responseFromLocalPg.isSuccess()) {
                return ResponseEntity.ok(responseFromLocalPg);
            }
        }
        return ResponseEntity.badRequest().body(responseFromFirestore);
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

