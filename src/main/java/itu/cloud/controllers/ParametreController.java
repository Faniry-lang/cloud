package itu.cloud.controllers;

import itu.cloud.dto.UpdateParametreRequest;
import itu.cloud.entities.Parametre;
import itu.cloud.service.ParametreService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/parametres")
public class ParametreController {

    private final ParametreService parametreService;

    public ParametreController(ParametreService parametreService) {
        this.parametreService = parametreService;
    }

    @GetMapping
    public ResponseEntity<?> getAllParametres() {
        try {
            List<Parametre> parametres = parametreService.getAllParametres();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", parametres.size());
            response.put("parametres", parametres);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors de la récupération des paramètres: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getParametreById(@PathVariable Integer id) {
        try {
            Parametre parametre = parametreService.getParametreById(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("parametre", parametre);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors de la récupération du paramètre: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateParametre(@PathVariable Integer id,
                                             @RequestBody UpdateParametreRequest request) {
        try {
            if (request.getValeur() == null || request.getValeur().isEmpty()) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse("La valeur du paramètre est requise"));
            }

            Parametre parametre = parametreService.updateParametre(id, request.getValeur());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Paramètre mis à jour avec succès");
            response.put("parametre", parametre);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors de la mise à jour du paramètre: " + e.getMessage()));
        }
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        return response;
    }
}
