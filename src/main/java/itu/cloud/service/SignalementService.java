package itu.cloud.service;

import itu.cloud.dto.ApiResponse;
import itu.cloud.dto.SignalementDTO;
import itu.cloud.entities.Entreprise;
import itu.cloud.entities.Signalement;
import itu.cloud.repositories.EntrepriseRepository;
import itu.cloud.repositories.SignalementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service hybride pour les Signalements.
 * Utilise Firebase par defaut, bascule en local si offline.
 */
@Service
public class SignalementService {

    private final SignalementRepository signalementRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final FirestoreService firestoreService;
    private final ConnectivityService connectivityService;
    private final JournalService journalService;

    public SignalementService(SignalementRepository signalementRepository,
                              EntrepriseRepository entrepriseRepository,
                              FirestoreService firestoreService,
                              ConnectivityService connectivityService,
                              JournalService journalService) {
        this.signalementRepository = signalementRepository;
        this.entrepriseRepository = entrepriseRepository;
        this.firestoreService = firestoreService;
        this.connectivityService = connectivityService;
        this.journalService = journalService;
    }

    /**
     * Recupere tous les signalements
     */
    public ApiResponse<List<SignalementDTO>> getAll() {
        boolean isOnline = connectivityService.isOnline();
        String mode = isOnline ? "ONLINE" : "OFFLINE";

        try {
            List<SignalementDTO> signalements;

            if (isOnline) {
                List<Map<String, Object>> firestoreData = firestoreService.getAllSignalements();
                signalements = firestoreData.stream()
                        .map(this::mapFromFirestore)
                        .collect(Collectors.toList());
            } else {
                signalements = signalementRepository.findAll().stream()
                        .filter(s -> s.getDateSuppression() == null)
                        .map(this::mapToDTO)
                        .collect(Collectors.toList());
            }

            return ApiResponse.success(signalements, mode);
        } catch (Exception e) {
            if (isOnline) {
                List<SignalementDTO> signalements = signalementRepository.findAll().stream()
                        .filter(s -> s.getDateSuppression() == null)
                        .map(this::mapToDTO)
                        .collect(Collectors.toList());
                return ApiResponse.success(signalements, "Basculement en mode OFFLINE (erreur Firebase)", "OFFLINE");
            }
            return ApiResponse.error("Erreur lors de la recuperation: " + e.getMessage(), mode);
        }
    }

    /**
     * Recupere un signalement par ID
     */
    public ApiResponse<SignalementDTO> getById(Integer id) {
        boolean isOnline = connectivityService.isOnline();
        String mode = isOnline ? "ONLINE" : "OFFLINE";

        try {
            SignalementDTO signalement = null;

            if (isOnline) {
                Optional<Map<String, Object>> firestoreData = firestoreService.getSignalement(id);
                if (firestoreData.isPresent()) {
                    signalement = mapFromFirestore(firestoreData.get());
                }
            } else {
                Optional<Signalement> localData = signalementRepository.findById(id);
                if (localData.isPresent() && localData.get().getDateSuppression() == null) {
                    signalement = mapToDTO(localData.get());
                }
            }

            if (signalement != null) {
                return ApiResponse.success(signalement, mode);
            } else {
                return ApiResponse.error("Signalement non trouve", mode);
            }
        } catch (Exception e) {
            if (isOnline) {
                Optional<Signalement> localData = signalementRepository.findById(id);
                if (localData.isPresent() && localData.get().getDateSuppression() == null) {
                    return ApiResponse.success(mapToDTO(localData.get()), "OFFLINE");
                }
            }
            return ApiResponse.error("Erreur: " + e.getMessage(), mode);
        }
    }

    /**
     * Recupere les signalements par entreprise
     */
    public ApiResponse<List<SignalementDTO>> getByEntreprise(Integer idEntreprise) {
        boolean isOnline = connectivityService.isOnline();
        String mode = isOnline ? "ONLINE" : "OFFLINE";

        try {
            List<SignalementDTO> signalements;

            if (isOnline) {
                List<Map<String, Object>> firestoreData = firestoreService.getSignalementsByEntreprise(idEntreprise);
                signalements = firestoreData.stream()
                        .map(this::mapFromFirestore)
                        .collect(Collectors.toList());
            } else {
                signalements = signalementRepository.findAll().stream()
                        .filter(s -> s.getDateSuppression() == null &&
                                     s.getIdEntreprise() != null &&
                                     s.getIdEntreprise().getId().equals(idEntreprise))
                        .map(this::mapToDTO)
                        .collect(Collectors.toList());
            }

            return ApiResponse.success(signalements, mode);
        } catch (Exception e) {
            return ApiResponse.error("Erreur: " + e.getMessage(), mode);
        }
    }

    /**
     * Cree un nouveau signalement
     */
    @Transactional
    public ApiResponse<SignalementDTO> create(SignalementDTO dto) {
        boolean isOnline = connectivityService.isOnline();
        String mode = isOnline ? "ONLINE" : "OFFLINE";

        try {
            if (dto.getIdEntreprise() == null) {
                return ApiResponse.error("L'ID de l'entreprise est requis", mode);
            }

            Optional<Entreprise> optEntreprise = entrepriseRepository.findById(dto.getIdEntreprise());
            if (optEntreprise.isEmpty()) {
                return ApiResponse.error("Entreprise non trouvee", mode);
            }

            Signalement signalement = new Signalement();
            signalement.setDescription(dto.getDescription());
            signalement.setSurfaceM2(dto.getSurfaceM2());
            signalement.setBudget(dto.getBudget());
            signalement.setIdEntreprise(optEntreprise.get());
            signalement.setVersion(1);
            signalement.setDateCreation(Instant.now());
            signalement = signalementRepository.save(signalement);

            if (isOnline) {
                firestoreService.saveSignalement(
                    signalement.getId(),
                    signalement.getDescription(),
                    signalement.getSurfaceM2(),
                    signalement.getBudget(),
                    signalement.getIdEntreprise().getId(),
                    signalement.getVersion(),
                    signalement.getDateCreation()
                );
            }

            journalService.logCreationSignalement(signalement.getId(), signalement.getDescription(),
                signalement.getIdEntreprise().getId());

            return ApiResponse.success(mapToDTO(signalement), "Signalement cree", mode);
        } catch (Exception e) {
            if (isOnline) {
                try {
                    return createOffline(dto);
                } catch (Exception ex) {
                    return ApiResponse.error("Erreur lors de la creation: " + ex.getMessage(), "OFFLINE");
                }
            }
            return ApiResponse.error("Erreur lors de la creation: " + e.getMessage(), mode);
        }
    }

    private ApiResponse<SignalementDTO> createOffline(SignalementDTO dto) {
        Optional<Entreprise> optEntreprise = entrepriseRepository.findById(dto.getIdEntreprise());
        if (optEntreprise.isEmpty()) {
            return ApiResponse.error("Entreprise non trouvee", "OFFLINE");
        }

        Signalement signalement = new Signalement();
        signalement.setDescription(dto.getDescription());
        signalement.setSurfaceM2(dto.getSurfaceM2());
        signalement.setBudget(dto.getBudget());
        signalement.setIdEntreprise(optEntreprise.get());
        signalement.setVersion(1);
        signalement.setDateCreation(Instant.now());
        signalement = signalementRepository.save(signalement);

        journalService.logCreationSignalement(signalement.getId(), signalement.getDescription(),
            signalement.getIdEntreprise().getId());

        return ApiResponse.success(mapToDTO(signalement), "Signalement cree en local (Firebase indisponible)", "OFFLINE");
    }

    /**
     * Met a jour un signalement
     */
    @Transactional
    public ApiResponse<SignalementDTO> update(Integer id, SignalementDTO dto) {
        boolean isOnline = connectivityService.isOnline();
        String mode = isOnline ? "ONLINE" : "OFFLINE";

        try {
            Optional<Signalement> optSignalement = signalementRepository.findById(id);
            if (optSignalement.isEmpty() || optSignalement.get().getDateSuppression() != null) {
                return ApiResponse.error("Signalement non trouve", mode);
            }

            Signalement signalement = optSignalement.get();

            if (dto.getDescription() != null) {
                signalement.setDescription(dto.getDescription());
            }
            if (dto.getSurfaceM2() != null) {
                signalement.setSurfaceM2(dto.getSurfaceM2());
            }
            if (dto.getBudget() != null) {
                signalement.setBudget(dto.getBudget());
            }
            if (dto.getIdEntreprise() != null) {
                Optional<Entreprise> optEntreprise = entrepriseRepository.findById(dto.getIdEntreprise());
                if (optEntreprise.isPresent()) {
                    signalement.setIdEntreprise(optEntreprise.get());
                }
            }

            signalement.setVersion(signalement.getVersion() != null ? signalement.getVersion() + 1 : 1);
            signalement.setDateMisAJour(Instant.now());
            signalement = signalementRepository.save(signalement);

            if (isOnline) {
                firestoreService.saveSignalement(
                    signalement.getId(),
                    signalement.getDescription(),
                    signalement.getSurfaceM2(),
                    signalement.getBudget(),
                    signalement.getIdEntreprise().getId(),
                    signalement.getVersion(),
                    signalement.getDateCreation()
                );
            }

            journalService.logUpdateSignalement(signalement.getId(), signalement.getDescription(), signalement.getVersion());

            return ApiResponse.success(mapToDTO(signalement), "Signalement mis a jour", mode);
        } catch (Exception e) {
            return ApiResponse.error("Erreur lors de la mise a jour: " + e.getMessage(), mode);
        }
    }

    /**
     * Supprime un signalement (soft delete)
     */
    @Transactional
    public ApiResponse<Void> delete(Integer id) {
        boolean isOnline = connectivityService.isOnline();
        String mode = isOnline ? "ONLINE" : "OFFLINE";

        try {
            Optional<Signalement> optSignalement = signalementRepository.findById(id);
            if (optSignalement.isEmpty() || optSignalement.get().getDateSuppression() != null) {
                return ApiResponse.error("Signalement non trouve", mode);
            }

            Signalement signalement = optSignalement.get();
            signalement.setDateSuppression(Instant.now());
            signalement.setDateMisAJour(Instant.now());
            signalementRepository.save(signalement);

            if (isOnline) {
                firestoreService.deleteSignalement(id);
            }

            journalService.logDeleteSignalement(id);

            return ApiResponse.success(null, "Signalement supprime", mode);
        } catch (Exception e) {
            return ApiResponse.error("Erreur lors de la suppression: " + e.getMessage(), mode);
        }
    }

    // ==================== MAPPING ====================

    private SignalementDTO mapToDTO(Signalement entity) {
        return SignalementDTO.builder()
                .id(entity.getId())
                .description(entity.getDescription())
                .surfaceM2(entity.getSurfaceM2())
                .budget(entity.getBudget())
                .idEntreprise(entity.getIdEntreprise() != null ? entity.getIdEntreprise().getId() : null)
                .entrepriseNom(entity.getIdEntreprise() != null ? entity.getIdEntreprise().getNom() : null)
                .version(entity.getVersion())
                .dateCreation(entity.getDateCreation())
                .dateMisAJour(entity.getDateMisAJour())
                .build();
    }

    private SignalementDTO mapFromFirestore(Map<String, Object> data) {
        return SignalementDTO.builder()
                .id(data.get("id") != null ? ((Number) data.get("id")).intValue() : null)
                .description((String) data.get("description"))
                .surfaceM2(data.get("surfaceM2") != null ? new BigDecimal((String) data.get("surfaceM2")) : null)
                .budget(data.get("budget") != null ? new BigDecimal((String) data.get("budget")) : null)
                .idEntreprise(data.get("idEntreprise") != null ? ((Number) data.get("idEntreprise")).intValue() : null)
                .version(data.get("version") != null ? ((Number) data.get("version")).intValue() : null)
                .dateCreation(data.get("dateCreation") != null ? Instant.parse((String) data.get("dateCreation")) : null)
                .dateMisAJour(data.get("dateMiseAJour") != null ? Instant.parse((String) data.get("dateMiseAJour")) : null)
                .build();
    }
}

