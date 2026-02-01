package itu.cloud.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import itu.cloud.collections.SignalementCollection;
import itu.cloud.entities.HistoriqueStatutSignalement;
import itu.cloud.entities.Signalement;
import itu.cloud.entities.StatutSignalement;
import itu.cloud.repositories.HistoriqueStatutSignalementRepository;
import itu.cloud.repositories.SignalementRepository;
import itu.cloud.repositories.StatutSignalementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
public class SignalementService {

    private final SignalementRepository signalementRepository;
    private final StatutSignalementRepository statutSignalementRepository;
    private final HistoriqueStatutSignalementRepository historiqueRepository;
    private final JournalService journalService;
    private final ObjectMapper objectMapper;

    public SignalementService(SignalementRepository signalementRepository,
                             StatutSignalementRepository statutSignalementRepository,
                             HistoriqueStatutSignalementRepository historiqueRepository,
                             JournalService journalService) {
        this.signalementRepository = signalementRepository;
        this.statutSignalementRepository = statutSignalementRepository;
        this.historiqueRepository = historiqueRepository;
        this.journalService = journalService;
        this.objectMapper = new ObjectMapper();
    }

    @Transactional
    public SignalementCollection changeStatus(Integer signalementId, Integer statusLevel) {
        Signalement signalement = signalementRepository.findById(signalementId)
                .orElseThrow(() -> new RuntimeException("Signalement non trouvé"));
        StatutSignalement statut = statutSignalementRepository.findByNiveau(statusLevel)
                .orElseGet(() -> {
                    StatutSignalement newStatut = new StatutSignalement();
                    newStatut.setNiveau(statusLevel);
                    String nomStatut = switch (statusLevel) {
                        case 1 -> "NOUVEAU";
                        case 2 -> "EN_COURS";
                        case 3 -> "TERMINE";
                        default -> "STATUT_" + statusLevel;
                    };
                    newStatut.setNom(nomStatut);
                    return statutSignalementRepository.save(newStatut);
                });

        HistoriqueStatutSignalement historique = new HistoriqueStatutSignalement();
        historique.setIdSignalement(signalement);
        historique.setIdStatutSignalement(statut);
        historique.setDateCreation(LocalDateTime.now());
        historiqueRepository.save(historique);

        signalement.setDateMisAJour(LocalDateTime.now());
        signalementRepository.save(signalement);

        SignalementCollection signalementCollection = convertToCollection(signalement);
        signalementCollection.setStatut(statusLevel);

        Map<String, Object> donnees = objectMapper.convertValue(signalementCollection, Map.class);
        donnees.put("statut", statusLevel);

        journalService.journaliser(
            "SignalementCollection",
            "UPDATE",
            signalement.getFirebaseUid(),
            donnees,
            signalement.getVersion()
        );

        return signalementCollection;
    }

    private SignalementCollection convertToCollection(Signalement signalement) {
        SignalementCollection collection = new SignalementCollection();
        collection.setPostgres_id(signalement.getId());
        collection.setFirebaseUid(signalement.getFirebaseUid());
        collection.setDescription(signalement.getDescription());
        collection.setSurfaceM2(signalement.getSurfaceM2());
        collection.setBudget(signalement.getBudget());
        collection.setVersion(signalement.getVersion());

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
}
