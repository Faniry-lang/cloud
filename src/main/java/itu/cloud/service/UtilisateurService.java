package itu.cloud.service;

import itu.cloud.dto.ApiResponse;
import itu.cloud.dto.UtilisateurDTO;
import itu.cloud.entities.Utilisateur;
import itu.cloud.repositories.UtilisateurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service hybride pour les Utilisateurs (lecture/gestion).
 * L'inscription et connexion sont gerees par HybridAuthService.
 * Ce service est pour les operations CRUD administratives.
 */
@Service
public class UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final FirestoreService firestoreService;
    private final ConnectivityService connectivityService;
    private final JournalService journalService;

    public UtilisateurService(UtilisateurRepository utilisateurRepository,
                              FirestoreService firestoreService,
                              ConnectivityService connectivityService,
                              JournalService journalService) {
        this.utilisateurRepository = utilisateurRepository;
        this.firestoreService = firestoreService;
        this.connectivityService = connectivityService;
        this.journalService = journalService;
    }

    /**
     * Recupere tous les utilisateurs
     */
    public ApiResponse<List<UtilisateurDTO>> getAll() {
        boolean isOnline = connectivityService.isOnline();
        String mode = isOnline ? "ONLINE" : "OFFLINE";

        try {
            List<UtilisateurDTO> utilisateurs;

            if (isOnline) {
                List<Map<String, Object>> firestoreData = firestoreService.getAllUtilisateurs();
                utilisateurs = firestoreData.stream()
                        .map(this::mapFromFirestore)
                        .collect(Collectors.toList());
            } else {
                utilisateurs = utilisateurRepository.findAll().stream()
                        .filter(u -> u.getDateSuppression() == null)
                        .map(this::mapToDTO)
                        .collect(Collectors.toList());
            }

            return ApiResponse.success(utilisateurs, mode);
        } catch (Exception e) {
            if (isOnline) {
                List<UtilisateurDTO> utilisateurs = utilisateurRepository.findAll().stream()
                        .filter(u -> u.getDateSuppression() == null)
                        .map(this::mapToDTO)
                        .collect(Collectors.toList());
                return ApiResponse.success(utilisateurs, "Basculement en mode OFFLINE (erreur Firebase)", "OFFLINE");
            }
            return ApiResponse.error("Erreur lors de la recuperation: " + e.getMessage(), mode);
        }
    }

    /**
     * Recupere un utilisateur par ID
     */
    public ApiResponse<UtilisateurDTO> getById(Integer id) {
        boolean isOnline = connectivityService.isOnline();
        String mode = isOnline ? "ONLINE" : "OFFLINE";

        try {
            UtilisateurDTO utilisateur = null;

            if (isOnline) {
                Optional<Map<String, Object>> firestoreData = firestoreService.getUtilisateur(id);
                if (firestoreData.isPresent()) {
                    utilisateur = mapFromFirestore(firestoreData.get());
                }
            } else {
                Optional<Utilisateur> localData = utilisateurRepository.findById(id);
                if (localData.isPresent() && localData.get().getDateSuppression() == null) {
                    utilisateur = mapToDTO(localData.get());
                }
            }

            if (utilisateur != null) {
                return ApiResponse.success(utilisateur, mode);
            } else {
                return ApiResponse.error("Utilisateur non trouve", mode);
            }
        } catch (Exception e) {
            if (isOnline) {
                Optional<Utilisateur> localData = utilisateurRepository.findById(id);
                if (localData.isPresent() && localData.get().getDateSuppression() == null) {
                    return ApiResponse.success(mapToDTO(localData.get()), "OFFLINE");
                }
            }
            return ApiResponse.error("Erreur: " + e.getMessage(), mode);
        }
    }

    /**
     * Recupere un utilisateur par email
     */
    public ApiResponse<UtilisateurDTO> getByEmail(String email) {
        boolean isOnline = connectivityService.isOnline();
        String mode = isOnline ? "ONLINE" : "OFFLINE";

        try {
            UtilisateurDTO utilisateur = null;

            if (isOnline) {
                Optional<Map<String, Object>> firestoreData = firestoreService.getUtilisateurByEmail(email);
                if (firestoreData.isPresent()) {
                    utilisateur = mapFromFirestore(firestoreData.get());
                }
            } else {
                Optional<Utilisateur> localData = utilisateurRepository.findByEmail(email);
                if (localData.isPresent() && localData.get().getDateSuppression() == null) {
                    utilisateur = mapToDTO(localData.get());
                }
            }

            if (utilisateur != null) {
                return ApiResponse.success(utilisateur, mode);
            } else {
                return ApiResponse.error("Utilisateur non trouve", mode);
            }
        } catch (Exception e) {
            if (isOnline) {
                Optional<Utilisateur> localData = utilisateurRepository.findByEmail(email);
                if (localData.isPresent() && localData.get().getDateSuppression() == null) {
                    return ApiResponse.success(mapToDTO(localData.get()), "OFFLINE");
                }
            }
            return ApiResponse.error("Erreur: " + e.getMessage(), mode);
        }
    }

    /**
     * Met a jour un utilisateur (informations de base uniquement, pas le mot de passe)
     */
    @Transactional
    public ApiResponse<UtilisateurDTO> update(Integer id, UtilisateurDTO dto) {
        boolean isOnline = connectivityService.isOnline();
        String mode = isOnline ? "ONLINE" : "OFFLINE";

        try {
            Optional<Utilisateur> optUtilisateur = utilisateurRepository.findById(id);
            if (optUtilisateur.isEmpty() || optUtilisateur.get().getDateSuppression() != null) {
                return ApiResponse.error("Utilisateur non trouve", mode);
            }

            Utilisateur utilisateur = optUtilisateur.get();

            if (dto.getNom() != null) {
                utilisateur.setNom(dto.getNom());
            }
            if (dto.getActif() != null) {
                utilisateur.setActif(dto.getActif());
            }

            utilisateur.setVersion(utilisateur.getVersion() != null ? utilisateur.getVersion() + 1 : 1);
            utilisateur.setDateMisAJour(Instant.now());
            utilisateur = utilisateurRepository.save(utilisateur);

            if (isOnline) {
                firestoreService.saveUtilisateur(
                    utilisateur.getId(),
                    utilisateur.getEmail(),
                    utilisateur.getNom(),
                    utilisateur.getFirebaseUid(),
                    utilisateur.getVersion(),
                    utilisateur.getDateCreation()
                );
            }

            // Journaliser la mise à jour
            journalService.logConnexionReussie(utilisateur.getId(), utilisateur.getEmail(), "UPDATE");

            return ApiResponse.success(mapToDTO(utilisateur), "Utilisateur mis a jour", mode);
        } catch (Exception e) {
            return ApiResponse.error("Erreur lors de la mise a jour: " + e.getMessage(), mode);
        }
    }

    /**
     * Desactive un utilisateur (soft delete)
     */
    @Transactional
    public ApiResponse<Void> delete(Integer id) {
        boolean isOnline = connectivityService.isOnline();
        String mode = isOnline ? "ONLINE" : "OFFLINE";

        try {
            Optional<Utilisateur> optUtilisateur = utilisateurRepository.findById(id);
            if (optUtilisateur.isEmpty() || optUtilisateur.get().getDateSuppression() != null) {
                return ApiResponse.error("Utilisateur non trouve", mode);
            }

            Utilisateur utilisateur = optUtilisateur.get();
            utilisateur.setDateSuppression(Instant.now());
            utilisateur.setActif(false);
            utilisateur.setDateMisAJour(Instant.now());
            utilisateurRepository.save(utilisateur);

            // Note: on ne supprime pas l'utilisateur Firebase, juste la reference locale
            // La suppression Firebase serait geree separement si necessaire

            return ApiResponse.success(null, "Utilisateur desactive", mode);
        } catch (Exception e) {
            return ApiResponse.error("Erreur lors de la desactivation: " + e.getMessage(), mode);
        }
    }

    /**
     * Recupere tous les utilisateurs actuellement bloques
     */
    public ApiResponse<List<UtilisateurDTO>> getAllBlocked() {
        boolean isOnline = connectivityService.isOnline();
        String mode = isOnline ? "ONLINE" : "OFFLINE";

        try {
            // On utilise toujours le local car le blocage est géré localement
            List<UtilisateurDTO> utilisateurs = utilisateurRepository
                    .findByBloqueJusquaAfterAndDateSuppressionIsNull(Instant.now())
                    .stream()
                    .map(this::mapToDTO)
                    .collect(Collectors.toList());

            return ApiResponse.success(utilisateurs, "Liste des utilisateurs bloques", mode);
        } catch (Exception e) {
            return ApiResponse.error("Erreur lors de la recuperation des utilisateurs bloques: " + e.getMessage(), mode);
        }
    }

    /**
     * Bloque un utilisateur pour une duree specifiee (en minutes)
     */
    @Transactional
    public ApiResponse<UtilisateurDTO> blockUser(Integer id, Integer dureeMinutes) {
        boolean isOnline = connectivityService.isOnline();
        String mode = isOnline ? "ONLINE" : "OFFLINE";

        try {
            Optional<Utilisateur> optUtilisateur = utilisateurRepository.findById(id);
            if (optUtilisateur.isEmpty() || optUtilisateur.get().getDateSuppression() != null) {
                return ApiResponse.error("Utilisateur non trouve", mode);
            }

            Utilisateur utilisateur = optUtilisateur.get();

            // Calculer la date de fin de blocage
            Instant bloqueJusqua = Instant.now().plusSeconds(dureeMinutes * 60L);
            utilisateur.setBloqueJusqua(bloqueJusqua);
            utilisateur.setDateMisAJour(Instant.now());
            utilisateur = utilisateurRepository.save(utilisateur);

            // Journaliser le blocage
            journalService.logBlocageCompte(utilisateur.getId(), utilisateur.getEmail(), bloqueJusqua);

            return ApiResponse.success(mapToDTO(utilisateur),
                    "Utilisateur bloque jusqu'a " + bloqueJusqua.toString(), mode);
        } catch (Exception e) {
            return ApiResponse.error("Erreur lors du blocage: " + e.getMessage(), mode);
        }
    }

    /**
     * Debloque un utilisateur
     */
    @Transactional
    public ApiResponse<UtilisateurDTO> unblockUser(Integer id) {
        boolean isOnline = connectivityService.isOnline();
        String mode = isOnline ? "ONLINE" : "OFFLINE";

        try {
            Optional<Utilisateur> optUtilisateur = utilisateurRepository.findById(id);
            if (optUtilisateur.isEmpty() || optUtilisateur.get().getDateSuppression() != null) {
                return ApiResponse.error("Utilisateur non trouve", mode);
            }

            Utilisateur utilisateur = optUtilisateur.get();
            utilisateur.setBloqueJusqua(null);
            utilisateur.setTentativesEchouees(0);
            utilisateur.setDateMisAJour(Instant.now());
            utilisateur = utilisateurRepository.save(utilisateur);

            // Journaliser le deblocage
            journalService.logDeblocageCompte(utilisateur.getId(), utilisateur.getEmail());

            return ApiResponse.success(mapToDTO(utilisateur), "Utilisateur debloque", mode);
        } catch (Exception e) {
            return ApiResponse.error("Erreur lors du deblocage: " + e.getMessage(), mode);
        }
    }

    // exposer findById pour les filtres/auth
    public Optional<Utilisateur> findById(Integer id) {
        return utilisateurRepository.findById(id);
    }

    // exposer findByEmail pour les filtres/auth
    public Optional<Utilisateur> findByEmail(String email) {
        return utilisateurRepository.findByEmail(email);
    }

    // ==================== MAPPING ====================

    private UtilisateurDTO mapToDTO(Utilisateur entity) {
        return UtilisateurDTO.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .nom(entity.getNom())
                .firebaseUid(entity.getFirebaseUid())
                .actif(entity.getActif())
                .version(entity.getVersion())
                .tentativesEchouees(entity.getTentativesEchouees())
                .bloqueJusqua(entity.getBloqueJusqua())
                .dateCreation(entity.getDateCreation())
                .dateMisAJour(entity.getDateMisAJour())
                .build();
    }

    private UtilisateurDTO mapFromFirestore(Map<String, Object> data) {
        return UtilisateurDTO.builder()
                .id(data.get("id") != null ? ((Number) data.get("id")).intValue() : null)
                .email((String) data.get("email"))
                .nom((String) data.get("nom"))
                .firebaseUid((String) data.get("firebaseUid"))
                .version(data.get("version") != null ? ((Number) data.get("version")).intValue() : null)
                .dateCreation(data.get("dateCreation") != null ? Instant.parse((String) data.get("dateCreation")) : null)
                .dateMisAJour(data.get("dateMiseAJour") != null ? Instant.parse((String) data.get("dateMiseAJour")) : null)
                .build();
    }
}

