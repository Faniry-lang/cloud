package itu.cloud.service;

import itu.cloud.dto.ApiResponse;
import itu.cloud.dto.StatutDTO;
import itu.cloud.entities.Statut;
import itu.cloud.repositories.StatutRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service hybride pour les Statuts.
 * Utilise Firebase par defaut, bascule en local si offline.
 */
@Service
public class StatutService {

    private final StatutRepository statutRepository;
    private final FirestoreService firestoreService;
    private final ConnectivityService connectivityService;
    private final JournalService journalService;

    public StatutService(StatutRepository statutRepository,
                         FirestoreService firestoreService,
                         ConnectivityService connectivityService,
                         JournalService journalService) {
        this.statutRepository = statutRepository;
        this.firestoreService = firestoreService;
        this.connectivityService = connectivityService;
        this.journalService = journalService;
    }

    /**
     * Recupere tous les statuts
     */
    public ApiResponse<List<StatutDTO>> getAll() {
        boolean isOnline = connectivityService.isOnline();
        String mode = isOnline ? "ONLINE" : "OFFLINE";

        try {
            List<StatutDTO> statuts;

            if (isOnline) {
                List<Map<String, Object>> firestoreData = firestoreService.getAllStatuts();
                statuts = firestoreData.stream()
                        .map(this::mapFromFirestore)
                        .collect(Collectors.toList());
            } else {
                statuts = statutRepository.findAll().stream()
                        .filter(s -> s.getDateSuppression() == null)
                        .map(this::mapToDTO)
                        .collect(Collectors.toList());
            }

            return ApiResponse.success(statuts, mode);
        } catch (Exception e) {
            if (isOnline) {
                List<StatutDTO> statuts = statutRepository.findAll().stream()
                        .filter(s -> s.getDateSuppression() == null)
                        .map(this::mapToDTO)
                        .collect(Collectors.toList());
                return ApiResponse.success(statuts, "Basculement en mode OFFLINE (erreur Firebase)", "OFFLINE");
            }
            return ApiResponse.error("Erreur lors de la recuperation: " + e.getMessage(), mode);
        }
    }

    /**
     * Recupere un statut par ID
     */
    public ApiResponse<StatutDTO> getById(Integer id) {
        boolean isOnline = connectivityService.isOnline();
        String mode = isOnline ? "ONLINE" : "OFFLINE";

        try {
            StatutDTO statut = null;

            if (isOnline) {
                Optional<Map<String, Object>> firestoreData = firestoreService.getStatut(id);
                if (firestoreData.isPresent()) {
                    statut = mapFromFirestore(firestoreData.get());
                }
            } else {
                Optional<Statut> localData = statutRepository.findById(id);
                if (localData.isPresent() && localData.get().getDateSuppression() == null) {
                    statut = mapToDTO(localData.get());
                }
            }

            if (statut != null) {
                return ApiResponse.success(statut, mode);
            } else {
                return ApiResponse.error("Statut non trouve", mode);
            }
        } catch (Exception e) {
            if (isOnline) {
                Optional<Statut> localData = statutRepository.findById(id);
                if (localData.isPresent() && localData.get().getDateSuppression() == null) {
                    return ApiResponse.success(mapToDTO(localData.get()), "OFFLINE");
                }
            }
            return ApiResponse.error("Erreur: " + e.getMessage(), mode);
        }
    }

    /**
     * Cree un nouveau statut
     */
    @Transactional
    public ApiResponse<StatutDTO> create(StatutDTO dto) {
        boolean isOnline = connectivityService.isOnline();
        String mode = isOnline ? "ONLINE" : "OFFLINE";

        try {
            if (dto.getDescription() == null || dto.getDescription().isBlank()) {
                return ApiResponse.error("La description est requise", mode);
            }

            // Verifier unicite
            if (statutRepository.findByDescription(dto.getDescription()).isPresent()) {
                return ApiResponse.error("Un statut avec cette description existe deja", mode);
            }

            Statut statut = new Statut();
            statut.setDescription(dto.getDescription());
            statut.setDateCreation(Instant.now());
            statut = statutRepository.save(statut);

            if (isOnline) {
                firestoreService.saveStatut(statut.getId(), statut.getDescription(), statut.getDateCreation());
            }

            journalService.logCreationStatut(statut.getId(), statut.getDescription());

            return ApiResponse.success(mapToDTO(statut), "Statut cree", mode);
        } catch (Exception e) {
            if (isOnline) {
                try {
                    Statut statut = new Statut();
                    statut.setDescription(dto.getDescription());
                    statut.setDateCreation(Instant.now());
                    statut = statutRepository.save(statut);
                    journalService.logCreationStatut(statut.getId(), statut.getDescription());
                    return ApiResponse.success(mapToDTO(statut), "Statut cree en local (Firebase indisponible)", "OFFLINE");
                } catch (Exception ex) {
                    return ApiResponse.error("Erreur lors de la creation: " + ex.getMessage(), "OFFLINE");
                }
            }
            return ApiResponse.error("Erreur lors de la creation: " + e.getMessage(), mode);
        }
    }

    /**
     * Met a jour un statut
     */
    @Transactional
    public ApiResponse<StatutDTO> update(Integer id, StatutDTO dto) {
        boolean isOnline = connectivityService.isOnline();
        String mode = isOnline ? "ONLINE" : "OFFLINE";

        try {
            if (dto.getDescription() == null || dto.getDescription().isBlank()) {
                return ApiResponse.error("La description est requise", mode);
            }

            Optional<Statut> optStatut = statutRepository.findById(id);
            if (optStatut.isEmpty() || optStatut.get().getDateSuppression() != null) {
                return ApiResponse.error("Statut non trouve", mode);
            }

            Statut statut = optStatut.get();
            statut.setDescription(dto.getDescription());
            statut.setDateMisAJour(Instant.now());
            statut = statutRepository.save(statut);

            if (isOnline) {
                firestoreService.saveStatut(statut.getId(), statut.getDescription(), statut.getDateCreation());
            }

            journalService.logUpdateStatut(statut.getId(), statut.getDescription());

            return ApiResponse.success(mapToDTO(statut), "Statut mis a jour", mode);
        } catch (Exception e) {
            return ApiResponse.error("Erreur lors de la mise a jour: " + e.getMessage(), mode);
        }
    }

    /**
     * Supprime un statut (soft delete)
     */
    @Transactional
    public ApiResponse<Void> delete(Integer id) {
        boolean isOnline = connectivityService.isOnline();
        String mode = isOnline ? "ONLINE" : "OFFLINE";

        try {
            Optional<Statut> optStatut = statutRepository.findById(id);
            if (optStatut.isEmpty() || optStatut.get().getDateSuppression() != null) {
                return ApiResponse.error("Statut non trouve", mode);
            }

            Statut statut = optStatut.get();
            statut.setDateSuppression(Instant.now());
            statut.setDateMisAJour(Instant.now());
            statutRepository.save(statut);

            if (isOnline) {
                firestoreService.deleteStatut(id);
            }

            journalService.logDeleteStatut(id, statut.getDescription());

            return ApiResponse.success(null, "Statut supprime", mode);
        } catch (Exception e) {
            return ApiResponse.error("Erreur lors de la suppression: " + e.getMessage(), mode);
        }
    }

    // ==================== MAPPING ====================

    private StatutDTO mapToDTO(Statut entity) {
        return StatutDTO.builder()
                .id(entity.getId())
                .description(entity.getDescription())
                .dateCreation(entity.getDateCreation())
                .dateMisAJour(entity.getDateMisAJour())
                .build();
    }

    private StatutDTO mapFromFirestore(Map<String, Object> data) {
        return StatutDTO.builder()
                .id(data.get("id") != null ? ((Number) data.get("id")).intValue() : null)
                .description((String) data.get("description"))
                .dateCreation(data.get("dateCreation") != null ? Instant.parse((String) data.get("dateCreation")) : null)
                .dateMisAJour(data.get("dateMiseAJour") != null ? Instant.parse((String) data.get("dateMiseAJour")) : null)
                .build();
    }
}

