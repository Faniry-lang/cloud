package itu.cloud.service;

import itu.cloud.dto.ApiResponse;
import itu.cloud.dto.RoleDTO;
import itu.cloud.entities.Role;
import itu.cloud.repositories.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service hybride pour les Roles.
 * Utilise Firebase par defaut, bascule en local si offline.
 */
@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final FirestoreService firestoreService;
    private final ConnectivityService connectivityService;
    private final JournalService journalService;

    public RoleService(RoleRepository roleRepository,
                       FirestoreService firestoreService,
                       ConnectivityService connectivityService,
                       JournalService journalService) {
        this.roleRepository = roleRepository;
        this.firestoreService = firestoreService;
        this.connectivityService = connectivityService;
        this.journalService = journalService;
    }

    /**
     * Recupere tous les roles
     */
    public ApiResponse<List<RoleDTO>> getAll() {
        boolean isOnline = connectivityService.isOnline();
        String mode = isOnline ? "ONLINE" : "OFFLINE";

        try {
            List<RoleDTO> roles;

            if (isOnline) {
                List<Map<String, Object>> firestoreData = firestoreService.getAllRoles();
                roles = firestoreData.stream()
                        .map(this::mapFromFirestore)
                        .collect(Collectors.toList());
            } else {
                roles = roleRepository.findAll().stream()
                        .filter(r -> r.getDateSuppression() == null)
                        .map(this::mapToDTO)
                        .collect(Collectors.toList());
            }

            return ApiResponse.success(roles, mode);
        } catch (Exception e) {
            if (isOnline) {
                List<RoleDTO> roles = roleRepository.findAll().stream()
                        .filter(r -> r.getDateSuppression() == null)
                        .map(this::mapToDTO)
                        .collect(Collectors.toList());
                return ApiResponse.success(roles, "Basculement en mode OFFLINE (erreur Firebase)", "OFFLINE");
            }
            return ApiResponse.error("Erreur lors de la recuperation: " + e.getMessage(), mode);
        }
    }

    /**
     * Recupere un role par ID
     */
    public ApiResponse<RoleDTO> getById(Integer id) {
        boolean isOnline = connectivityService.isOnline();
        String mode = isOnline ? "ONLINE" : "OFFLINE";

        try {
            RoleDTO role = null;

            if (isOnline) {
                Optional<Map<String, Object>> firestoreData = firestoreService.getRole(id);
                if (firestoreData.isPresent()) {
                    role = mapFromFirestore(firestoreData.get());
                }
            } else {
                Optional<Role> localData = roleRepository.findById(id);
                if (localData.isPresent() && localData.get().getDateSuppression() == null) {
                    role = mapToDTO(localData.get());
                }
            }

            if (role != null) {
                return ApiResponse.success(role, mode);
            } else {
                return ApiResponse.error("Role non trouve", mode);
            }
        } catch (Exception e) {
            if (isOnline) {
                Optional<Role> localData = roleRepository.findById(id);
                if (localData.isPresent() && localData.get().getDateSuppression() == null) {
                    return ApiResponse.success(mapToDTO(localData.get()), "OFFLINE");
                }
            }
            return ApiResponse.error("Erreur: " + e.getMessage(), mode);
        }
    }

    /**
     * Cree un nouveau role
     */
    @Transactional
    public ApiResponse<RoleDTO> create(RoleDTO dto) {
        boolean isOnline = connectivityService.isOnline();
        String mode = isOnline ? "ONLINE" : "OFFLINE";

        try {
            if (dto.getNom() == null || dto.getNom().isBlank()) {
                return ApiResponse.error("Le nom est requis", mode);
            }

            // Verifier unicite
            if (roleRepository.findByNom(dto.getNom()).isPresent()) {
                return ApiResponse.error("Un role avec ce nom existe deja", mode);
            }

            Role role = new Role();
            role.setNom(dto.getNom());
            role.setDateCreation(Instant.now());
            role = roleRepository.save(role);

            if (isOnline) {
                firestoreService.saveRole(role.getId(), role.getNom(), role.getDateCreation());
            }

            journalService.logCreationRole(role.getId(), role.getNom());

            return ApiResponse.success(mapToDTO(role), "Role cree", mode);
        } catch (Exception e) {
            if (isOnline) {
                try {
                    Role role = new Role();
                    role.setNom(dto.getNom());
                    role.setDateCreation(Instant.now());
                    role = roleRepository.save(role);
                    journalService.logCreationRole(role.getId(), role.getNom());
                    return ApiResponse.success(mapToDTO(role), "Role cree en local (Firebase indisponible)", "OFFLINE");
                } catch (Exception ex) {
                    return ApiResponse.error("Erreur lors de la creation: " + ex.getMessage(), "OFFLINE");
                }
            }
            return ApiResponse.error("Erreur lors de la creation: " + e.getMessage(), mode);
        }
    }

    /**
     * Met a jour un role
     */
    @Transactional
    public ApiResponse<RoleDTO> update(Integer id, RoleDTO dto) {
        boolean isOnline = connectivityService.isOnline();
        String mode = isOnline ? "ONLINE" : "OFFLINE";

        try {
            if (dto.getNom() == null || dto.getNom().isBlank()) {
                return ApiResponse.error("Le nom est requis", mode);
            }

            Optional<Role> optRole = roleRepository.findById(id);
            if (optRole.isEmpty() || optRole.get().getDateSuppression() != null) {
                return ApiResponse.error("Role non trouve", mode);
            }

            Role role = optRole.get();
            role.setNom(dto.getNom());
            role.setDateMisAJour(Instant.now());
            role = roleRepository.save(role);

            if (isOnline) {
                firestoreService.saveRole(role.getId(), role.getNom(), role.getDateCreation());
            }

            journalService.logUpdateRole(role.getId(), role.getNom());

            return ApiResponse.success(mapToDTO(role), "Role mis a jour", mode);
        } catch (Exception e) {
            return ApiResponse.error("Erreur lors de la mise a jour: " + e.getMessage(), mode);
        }
    }

    /**
     * Supprime un role (soft delete)
     */
    @Transactional
    public ApiResponse<Void> delete(Integer id) {
        boolean isOnline = connectivityService.isOnline();
        String mode = isOnline ? "ONLINE" : "OFFLINE";

        try {
            Optional<Role> optRole = roleRepository.findById(id);
            if (optRole.isEmpty() || optRole.get().getDateSuppression() != null) {
                return ApiResponse.error("Role non trouve", mode);
            }

            Role role = optRole.get();
            role.setDateSuppression(Instant.now());
            role.setDateMisAJour(Instant.now());
            roleRepository.save(role);

            if (isOnline) {
                firestoreService.deleteRole(id);
            }

            journalService.logDeleteRole(id, role.getNom());

            return ApiResponse.success(null, "Role supprime", mode);
        } catch (Exception e) {
            return ApiResponse.error("Erreur lors de la suppression: " + e.getMessage(), mode);
        }
    }

    // ==================== MAPPING ====================

    private RoleDTO mapToDTO(Role entity) {
        return RoleDTO.builder()
                .id(entity.getId())
                .nom(entity.getNom())
                .dateCreation(entity.getDateCreation())
                .dateMisAJour(entity.getDateMisAJour())
                .build();
    }

    private RoleDTO mapFromFirestore(Map<String, Object> data) {
        return RoleDTO.builder()
                .id(data.get("id") != null ? ((Number) data.get("id")).intValue() : null)
                .nom((String) data.get("nom"))
                .dateCreation(data.get("dateCreation") != null ? Instant.parse((String) data.get("dateCreation")) : null)
                .dateMisAJour(data.get("dateMiseAJour") != null ? Instant.parse((String) data.get("dateMiseAJour")) : null)
                .build();
    }
}

