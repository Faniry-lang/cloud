package itu.cloud.controllers;

import itu.cloud.collections.SignalementCollection;
import itu.cloud.dto.StatistiquesTraitementDTO;
import itu.cloud.entities.HistoriqueStatutSignalement;
import itu.cloud.entities.Signalement;
import itu.cloud.firebase.services.SignalementFirebaseService;
import itu.cloud.repositories.HistoriqueStatutSignalementRepository;
import itu.cloud.repositories.SignalementRepository;
import itu.cloud.service.SignalementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/signalements")
public class SignalementController {

    private final SignalementFirebaseService signalementFirebaseService;
    private final SignalementRepository signalementRepository;
    private final SignalementService signalementService;
    private final HistoriqueStatutSignalementRepository historiqueStatutSignalementRepository;

    public SignalementController(SignalementFirebaseService signalementFirebaseService,
            SignalementRepository signalementRepository, SignalementService signalementService,
            HistoriqueStatutSignalementRepository historiqueStatutSignalementRepository) {
        this.signalementFirebaseService = signalementFirebaseService;
        this.signalementRepository = signalementRepository;
        this.signalementService = signalementService;
        this.historiqueStatutSignalementRepository = historiqueStatutSignalementRepository;
    }

    @GetMapping
    public ResponseEntity<?> getAllSignalements() {
        try {
            List<Signalement> signalements = signalementRepository.findAll();
            List<SignalementCollection> signalementCollections = new ArrayList<>();

            int totalCount = signalements.size();
            double totalSurface = 0;
            double totalBudget = 0;
            int completedCount = 0;
            int inProgressCount = 0;

            for (Signalement signalement : signalements) {
                SignalementCollection collection = convertToCollection(signalement);
                signalementCollections.add(collection);
                totalSurface += signalement.getSurfaceM2();
                totalBudget += signalement.getBudget();

                Integer statut = collection.getStatut();
                if (statut != null) {
                    if (statut == 2) {
                        completedCount++;
                    } else if (statut == 1) {
                        inProgressCount++;
                    }
                }
            }

            double progress = totalCount > 0 ? ((completedCount + (inProgressCount * 0.5)) / totalCount) * 100 : 0;

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalCount", totalCount);
            stats.put("totalSurface", Math.round(totalSurface * 100.0) / 100.0);
            stats.put("totalBudget", Math.round(totalBudget * 100.0) / 100.0);
            stats.put("progress", Math.round(progress * 10.0) / 10.0);

            Map<String, Object> response = new HashMap<>();
            response.put("signalements", signalementCollections);
            response.put("stats", stats);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors de la récupération des signalements: " + e.getMessage()));
        }
    }

    @PutMapping("/manager/change-status/{signalementId}/{statusLevel}")
    public ResponseEntity<?> changeStatus(@PathVariable Integer signalementId,
            @PathVariable Integer statusLevel) {
                                         @PathVariable Integer statusLevel,
                                         @RequestBody(required = false) Map<String, String> requestBody) {
        try {
            String dateStr = requestBody != null ? requestBody.get("date") : null;
            SignalementCollection updatedSignalement = signalementService.changeStatus(signalementId, statusLevel, dateStr);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Statut du signalement mis à jour avec succès");
            response.put("signalement", updatedSignalement);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            System.out.println("[DEBUG SignalementController.changeStatus] RuntimeException: " + e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            System.out.println("[DEBUG SignalementController.changeStatus] Exception: " + e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors de la mise à jour du statut: " + e.getMessage()));
        }
    }

    private SignalementCollection convertToCollection(Signalement signalement) {
        SignalementCollection collection = new SignalementCollection();
        collection.setPostgres_id(signalement.getId());
        collection.setFirebaseUid(signalement.getFirebaseUid());
        collection.setDescription(signalement.getDescription());
        collection.setSurfaceM2(signalement.getSurfaceM2());
        collection.setBudget(signalement.getBudget());
        collection.setVersion(signalement.getVersion());

        Optional<HistoriqueStatutSignalement> hss = this.historiqueStatutSignalementRepository
                .findFirstByIdSignalementOrderByDateCreationDesc(signalement);
        collection.setStatut(
                hss.isPresent() ? hss.get().getIdStatutSignalement().getNiveau() : 1);

        if (signalement.getIdEntreprise() != null) {
            collection.setIdEntreprise(signalement.getIdEntreprise().getId());
        }

        if (signalement.getIdTypeSignalement() != null) {
            SignalementCollection.TypeSignalementDTO typeDTO = new SignalementCollection.TypeSignalementDTO();
            typeDTO.setId(String.valueOf(signalement.getIdTypeSignalement().getId()));
            typeDTO.setNom(signalement.getIdTypeSignalement().getNom());
            typeDTO.setIcone(signalement.getIdTypeSignalement().getIcone());
            collection.setIdTypeSignalement(typeDTO);
        }

        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
        if (signalement.getDateCreation() != null) {
            collection.setDateCreation(signalement.getDateCreation().format(formatter));
        }
        if (signalement.getDateMisAJour() != null) {
            collection.setDateMisAJour(signalement.getDateMisAJour().format(formatter));
        }
        if (signalement.getDateSuppression() != null) {
            collection.setDateSuppression(signalement.getDateSuppression().format(formatter));
        }

        if (signalement.getPoints() != null) {
            try {
                String pointStr = signalement.getPoints().toString();

                if (pointStr.contains("POINT")) {
                    String coords = pointStr.substring(pointStr.indexOf('(') + 1, pointStr.indexOf(')'));
                    String[] parts = coords.trim().split("\\s+");

                    if (parts.length >= 2) {
                        SignalementCollection.Location location = new SignalementCollection.Location();
                        location.setLng(Double.parseDouble(parts[0]));
                        location.setLat(Double.parseDouble(parts[1]));
                        collection.setLocation(location);
                    }
                }
            } catch (Exception e) {
                System.err.println("Erreur lors de la conversion du point geometry: " + e.getMessage());
            }
        }

        return collection;
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        return response;
    }

    @GetMapping("/stats/traitement")
    public ResponseEntity<StatistiquesTraitementDTO> getStatistiquesTraitement() {
        return ResponseEntity.ok(signalementService.getStatistiquesTraitement());
    }
}
