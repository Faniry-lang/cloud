package itu.cloud.service;

import itu.cloud.dto.ApiResponse;
import itu.cloud.dto.EntrepriseDTO;
import itu.cloud.entities.Entreprise;
import itu.cloud.repositories.EntrepriseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EntrepriseService {

    private final EntrepriseRepository entrepriseRepository;
    private final FirestoreService firestoreService;
    private final ConnectivityService connectivityService;
    private final JournalService journalService;

    public EntrepriseService(EntrepriseRepository entrepriseRepository,
                             FirestoreService firestoreService,
                             ConnectivityService connectivityService,
                             JournalService journalService) {
        this.entrepriseRepository = entrepriseRepository;
        this.firestoreService = firestoreService;
        this.connectivityService = connectivityService;
        this.journalService = journalService;
    }

    public ApiResponse<List<EntrepriseDTO>> getAll() {
        boolean isOnline = connectivityService.isOnline();
        String mode = isOnline ? "ONLINE" : "OFFLINE";

        try {
            List<EntrepriseDTO> entreprises;

            if (isOnline) {

                List<Map<String, Object>> firestoreData = firestoreService.getAllEntreprises();
                entreprises = firestoreData.stream()
                        .map(this::mapFromFirestore)
                        .collect(Collectors.toList());
            } else {

                entreprises = entrepriseRepository.findAll().stream()
                        .filter(e -> e.getDateSuppression() == null)
                        .map(this::mapToDTO)
                        .collect(Collectors.toList());
            }

            return ApiResponse.success(entreprises, mode);
        } catch (Exception e) {
            if (isOnline) {
                List<EntrepriseDTO> entreprises = entrepriseRepository.findAll().stream()
                        .filter(ent -> ent.getDateSuppression() == null)
                        .map(this::mapToDTO)
                        .collect(Collectors.toList());
                return ApiResponse.success(entreprises, "Basculement en mode OFFLINE (erreur Firebase)", "OFFLINE");
            }
            return ApiResponse.error("Erreur lors de la recuperation: " + e.getMessage(), mode);
        }
    }

    public ApiResponse<EntrepriseDTO> getById(Integer id) {
        boolean isOnline = connectivityService.isOnline();
        String mode = isOnline ? "ONLINE" : "OFFLINE";

        try {
            EntrepriseDTO entreprise = null;

            if (isOnline) {
                Optional<Map<String, Object>> firestoreData = firestoreService.getEntreprise(id);
                if (firestoreData.isPresent()) {
                    entreprise = mapFromFirestore(firestoreData.get());
                }
            } else {
                Optional<Entreprise> localData = entrepriseRepository.findById(id);
                if (localData.isPresent() && localData.get().getDateSuppression() == null) {
                    entreprise = mapToDTO(localData.get());
                }
            }

            if (entreprise != null) {
                return ApiResponse.success(entreprise, mode);
            } else {
                return ApiResponse.error("Entreprise non trouvee", mode);
            }
        } catch (Exception e) {
            if (isOnline) {
                Optional<Entreprise> localData = entrepriseRepository.findById(id);
                if (localData.isPresent() && localData.get().getDateSuppression() == null) {
                    return ApiResponse.success(mapToDTO(localData.get()), "OFFLINE");
                }
            }
            return ApiResponse.error("Erreur: " + e.getMessage(), mode);
        }
    }

    @Transactional
    public ApiResponse<EntrepriseDTO> create(EntrepriseDTO dto) {
        boolean isOnline = connectivityService.isOnline();
        String mode = isOnline ? "ONLINE" : "OFFLINE";

        try {
            if (dto.getNom() == null || dto.getNom().isBlank()) {
                return ApiResponse.error("Le nom est requis", mode);
            }

            if (isOnline) {
                Entreprise entreprise = new Entreprise();
                entreprise.setNom(dto.getNom());
                entreprise.setDateCreation(Instant.now());
                entreprise = entrepriseRepository.save(entreprise);

                firestoreService.saveEntreprise(entreprise.getId(), entreprise.getNom(), 1, entreprise.getDateCreation());

                journalService.logCreationEntreprise(entreprise.getId(), entreprise.getNom());

                return ApiResponse.success(mapToDTO(entreprise), "Entreprise creee", mode);
            } else {

                Entreprise entreprise = new Entreprise();
                entreprise.setNom(dto.getNom());
                entreprise.setDateCreation(Instant.now());
                entreprise = entrepriseRepository.save(entreprise);
                journalService.logCreationEntreprise(entreprise.getId(), entreprise.getNom());

                return ApiResponse.success(mapToDTO(entreprise), "Entreprise creee (sera synchronisee plus tard)", mode);
            }
        } catch (Exception e) {
            if (isOnline) {
                try {
                    Entreprise entreprise = new Entreprise();
                    entreprise.setNom(dto.getNom());
                    entreprise.setDateCreation(Instant.now());
                    entreprise = entrepriseRepository.save(entreprise);
                    journalService.logCreationEntreprise(entreprise.getId(), entreprise.getNom());
                    return ApiResponse.success(mapToDTO(entreprise), "Entreprise creee en local (Firebase indisponible)", "OFFLINE");
                } catch (Exception ex) {
                    return ApiResponse.error("Erreur lors de la creation: " + ex.getMessage(), "OFFLINE");
                }
            }
            return ApiResponse.error("Erreur lors de la creation: " + e.getMessage(), mode);
        }
    }

    @Transactional
    public ApiResponse<EntrepriseDTO> update(Integer id, EntrepriseDTO dto) {
        boolean isOnline = connectivityService.isOnline();
        String mode = isOnline ? "ONLINE" : "OFFLINE";

        try {
            if (dto.getNom() == null || dto.getNom().isBlank()) {
                return ApiResponse.error("Le nom est requis", mode);
            }

            Optional<Entreprise> optEntreprise = entrepriseRepository.findById(id);
            if (optEntreprise.isEmpty() || optEntreprise.get().getDateSuppression() != null) {
                return ApiResponse.error("Entreprise non trouvee", mode);
            }

            Entreprise entreprise = optEntreprise.get();
            entreprise.setNom(dto.getNom());
            entreprise.setDateMisAJour(Instant.now());
            entreprise = entrepriseRepository.save(entreprise);

            if (isOnline) {
                firestoreService.saveEntreprise(entreprise.getId(), entreprise.getNom(),
                    dto.getVersion() != null ? dto.getVersion() + 1 : 1, entreprise.getDateCreation());
            }

            journalService.logUpdateEntreprise(entreprise.getId(), entreprise.getNom(), dto.getVersion());

            return ApiResponse.success(mapToDTO(entreprise), "Entreprise mise a jour", mode);
        } catch (Exception e) {
            return ApiResponse.error("Erreur lors de la mise a jour: " + e.getMessage(), mode);
        }
    }

    @Transactional
    public ApiResponse<Void> delete(Integer id) {
        boolean isOnline = connectivityService.isOnline();
        String mode = isOnline ? "ONLINE" : "OFFLINE";

        try {
            Optional<Entreprise> optEntreprise = entrepriseRepository.findById(id);
            if (optEntreprise.isEmpty() || optEntreprise.get().getDateSuppression() != null) {
                return ApiResponse.error("Entreprise non trouvee", mode);
            }

            Entreprise entreprise = optEntreprise.get();
            entreprise.setDateSuppression(Instant.now());
            entreprise.setDateMisAJour(Instant.now());
            entrepriseRepository.save(entreprise);

            if (isOnline) {
                firestoreService.deleteEntreprise(id);
            }

            journalService.logDeleteEntreprise(id, entreprise.getNom());

            return ApiResponse.success(null, "Entreprise supprimee", mode);
        } catch (Exception e) {
            return ApiResponse.error("Erreur lors de la suppression: " + e.getMessage(), mode);
        }
    }


    private EntrepriseDTO mapToDTO(Entreprise entity) {
        return EntrepriseDTO.builder()
                .id(entity.getId())
                .nom(entity.getNom())
                .dateCreation(entity.getDateCreation())
                .dateMisAJour(entity.getDateMisAJour())
                .build();
    }

    private EntrepriseDTO mapFromFirestore(Map<String, Object> data) {
        return EntrepriseDTO.builder()
                .id(data.get("id") != null ? ((Number) data.get("id")).intValue() : null)
                .nom((String) data.get("nom"))
                .version(data.get("version") != null ? ((Number) data.get("version")).intValue() : null)
                .dateCreation(data.get("dateCreation") != null ? Instant.parse((String) data.get("dateCreation")) : null)
                .dateMisAJour(data.get("dateMiseAJour") != null ? Instant.parse((String) data.get("dateMiseAJour")) : null)
                .build();
    }
}

