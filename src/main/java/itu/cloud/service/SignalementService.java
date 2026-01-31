package itu.cloud.service;

import itu.cloud.dto.ApiResponse;
import itu.cloud.dto.SignalementDTO;
import itu.cloud.entities.Entreprise;
import itu.cloud.entities.Signalement;
import itu.cloud.helpers.ConversionUtils;
import itu.cloud.repositories.EntrepriseRepository;
import itu.cloud.repositories.SignalementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
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

    @PersistenceContext
    private EntityManager entityManager;

    private final DataSource dataSource;

    public SignalementService(SignalementRepository signalementRepository,
                              EntrepriseRepository entrepriseRepository,
                              FirestoreService firestoreService,
                              ConnectivityService connectivityService,
                              JournalService journalService,
                              DataSource dataSource) {
        this.signalementRepository = signalementRepository;
        this.entrepriseRepository = entrepriseRepository;
        this.firestoreService = firestoreService;
        this.connectivityService = connectivityService;
        this.journalService = journalService;
        this.dataSource = dataSource;
    }

    /**
     * Recupere tous les signalements
     */
    public ApiResponse<List<SignalementDTO>> getAll() {
        // online-first : on tente Firebase puis on fallback local
        try {
            List<Map<String, Object>> firestoreData = firestoreService.getAllSignalements();
            List<SignalementDTO> signalements = firestoreData.stream()
                    .map(this::mapFromFirestore)
                    .collect(Collectors.toList());

            return ApiResponse.success(signalements, "ONLINE");
        } catch (Exception e) {
            // fallback local
            List<SignalementDTO> signalements = signalementRepository.findAll().stream()
                    .filter(s -> s.getDateSuppression() == null)
                    .map(this::mapToDTO)
                    .collect(Collectors.toList());
            return ApiResponse.success(signalements, "Basculement en mode OFFLINE (erreur Firebase: "+e.getMessage()+")", "OFFLINE");
        }
    }

    public ApiResponse<SignalementDTO> getByIdFromFirebase(String firebaseId) {
        Optional<Map<String, Object>> firestoreData = firestoreService.getSignalementFromFirebase(firebaseId);
        if (firestoreData.isPresent()) {
            SignalementDTO dto = mapFromFirestore(firestoreData.get());
            return ApiResponse.success(dto, "ONLINE");
        }

        return ApiResponse.error("Signalement non trouve", "ONLINE");
    }

    public ApiResponse<SignalementDTO> getById(Integer id) {
        // online-first : essayer Firestore d'abord
        try {
            // si absent en ligne, chercher en local
            Optional<Signalement> localData = signalementRepository.findById(id);
            if (localData.isPresent() && localData.get().getDateSuppression() == null) {
                return ApiResponse.success(mapToDTO(localData.get()), "OFFLINE");
            }
            return ApiResponse.error("Signalement non trouve", "ONLINE");
        } catch (Exception e) {
            // erreur Firebase -> fallback local
            Optional<Signalement> localData = signalementRepository.findById(id);
            if (localData.isPresent() && localData.get().getDateSuppression() == null) {
                return ApiResponse.success(mapToDTO(localData.get()), "OFFLINE");
            }
            return ApiResponse.error("Erreur: " + e.getMessage(), "OFFLINE");
        }
    }

    /**
     * Recupere les signalements par entreprise
     */
    public ApiResponse<List<SignalementDTO>> getByEntreprise(Integer idEntreprise) {
        // online-first
        try {
            List<Map<String, Object>> firestoreData = firestoreService.getSignalementsByEntreprise(idEntreprise);
            List<SignalementDTO> signalements = firestoreData.stream()
                    .map(this::mapFromFirestore)
                    .collect(Collectors.toList());
            return ApiResponse.success(signalements, "ONLINE");
        } catch (Exception e) {
            // fallback local
            List<SignalementDTO> signalements = signalementRepository.findAll().stream()
                    .filter(s -> s.getDateSuppression() == null &&
                            s.getIdEntreprise() != null &&
                            s.getIdEntreprise().getId().equals(idEntreprise))
                    .map(this::mapToDTO)
                    .collect(Collectors.toList());
            return ApiResponse.success(signalements, "OFFLINE");
        }
    }

    /**
     * Cree un nouveau signalement
     */
    @Transactional
    public ApiResponse<SignalementDTO> create(SignalementDTO dto) {
        try {
            if (dto.getIdEntreprise() == null) {
                return ApiResponse.error("L'ID de l'entreprise est requis", "OFFLINE");
            }

            Optional<Entreprise> optEntreprise = entrepriseRepository.findById(dto.getIdEntreprise());
            if (optEntreprise.isEmpty()) {
                return ApiResponse.error("Entreprise non trouvee", "OFFLINE");
            }

            // on cree en local d'abord (garantit un id postgres) puis on tente d'envoyer en ligne
            Signalement signalement = new Signalement();
            signalement.setDescription(dto.getDescription());
            signalement.setSurfaceM2(dto.getSurfaceM2());
            signalement.setBudget(dto.getBudget());
            signalement.setIdEntreprise(optEntreprise.get());
            signalement.setVersion(1);
            signalement.setDateCreation(Instant.now());
            signalement = signalementRepository.save(signalement);

            // si DTO contient latitude/longitude, mettre a jour la colonne points (positional params)
            if (dto.getLatitude() != null && dto.getLongitude() != null) {
                try {
                    updatePointsInDb(signalement.getId(), dto.getLongitude(), dto.getLatitude());
                } catch (Exception e) {
                    // log but ignore
                }
            }

            // attempt to push to Firestore with lat/lng if available
            Double[] latLng = getLatLngFor(signalement.getId());
            try {
                firestoreService.saveSignalement(
                        signalement.getId(),
                        signalement.getDescription(),
                        signalement.getSurfaceM2(),
                        signalement.getBudget(),
                        signalement.getIdEntreprise().getId(),
                        signalement.getVersion(),
                        signalement.getDateCreation(),
                        latLng[0], // latitude
                        latLng[1]  // longitude
                );
            } catch (Exception ex) {
                // on ignore l'erreur ici : on a deja sauve en local, le push se fera via sync
            }

            journalService.logCreationSignalement(signalement.getId(), signalement.getDescription(),
                    signalement.getIdEntreprise().getId());

            return ApiResponse.success(mapToDTO(signalement), "Signalement cree", "LOCAL");
        } catch (Exception e) {
            return ApiResponse.error("Erreur lors de la creation: " + e.getMessage(), "OFFLINE");
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

    public ApiResponse<SignalementDTO> updateToFirebase(String id, SignalementDTO dto) {
        Optional<Map<String, Object>> firestoreData = firestoreService.getSignalementFromFirebase(id);
        if (firestoreData.isPresent()) {
            SignalementDTO remote = mapFromFirestore(firestoreData.get());

            if (dto.getDescription() != null) remote.setDescription(dto.getDescription());
            if (dto.getSurfaceM2() != null) remote.setSurfaceM2(dto.getSurfaceM2());
            if (dto.getBudget() != null) remote.setBudget(dto.getBudget());
            if (dto.getIdEntreprise() != null) remote.setIdEntreprise(dto.getIdEntreprise());

            remote.setVersion(remote.getVersion() != null ? remote.getVersion() + 1 : 1);
            remote.setDateMisAJour(Instant.now());

            firestoreService.saveSignalement(
                    remote.getFirebaseId(),
                    remote.getDescription(),
                    remote.getSurfaceM2(),
                    remote.getBudget(),
                    remote.getIdEntreprise(),
                    remote.getVersion(),
                    remote.getDateCreation(),
                    remote.getLongitude(),
                    remote.getLatitude()
            );
            return ApiResponse.success(remote, "Signalement mis a jour", "ONLINE");
        }
        return ApiResponse.error("Erreur lors de la mise à jour vers Firebase: ", "ONLINE");
    }

    @Transactional
    public ApiResponse<SignalementDTO> update(Integer id, SignalementDTO dto) {
        try {
            // si pas trouve en ligne ou erreur, on tente local
            Optional<Signalement> optSignalement = signalementRepository.findById(id);
            if (optSignalement.isEmpty() || optSignalement.get().getDateSuppression() != null) {
                return ApiResponse.error("Signalement non trouve", "LOCAL");
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

            // if DTO includes lat/lng update points (positional)
            if (dto.getLatitude() != null && dto.getLongitude() != null) {
                try {
                    updatePointsInDb(signalement.getId(), dto.getLongitude(), dto.getLatitude());
                } catch (Exception ignored) {}
            }

            try {
                // tenter d'envoyer vers Firebase si possible
                Double[] latLng = getLatLngFor(signalement.getId());
                firestoreService.saveSignalement(
                        signalement.getId(),
                        signalement.getDescription(),
                        signalement.getSurfaceM2(),
                        signalement.getBudget(),
                        signalement.getIdEntreprise().getId(),
                        signalement.getVersion(),
                        signalement.getDateCreation(),
                        latLng[0],
                        latLng[1]
                );
            } catch (Exception ignored) {}

            journalService.logUpdateSignalement(signalement.getId(), signalement.getDescription(), signalement.getVersion());

            return ApiResponse.success(mapToDTO(signalement), "Signalement mis a jour", "LOCAL");
        } catch (Exception e) {
            return ApiResponse.error("Erreur lors de la mise a jour: " + e.getMessage(), "LOCAL");
        }
    }

    /**
     * Supprime un signalement (soft delete)
     */
    @Transactional
    public ApiResponse<Void> delete(Integer id) {
        try {
            // online-first : tenter suppression en ligne
            try {
                Optional<Map<String, Object>> firestoreData = firestoreService.getSignalement(id);
                if (firestoreData.isPresent()) {
                    firestoreService.deleteSignalement(id);

                    // marquer en local si existe
                    signalementRepository.findById(id).ifPresent(s -> {
                        s.setDateSuppression(Instant.now());
                        s.setDateMisAJour(Instant.now());
                        signalementRepository.save(s);
                    });

                    journalService.logDeleteSignalement(id);
                    return ApiResponse.success(null, "Signalement supprime", "ONLINE");
                }
            } catch (Exception ex) {
                System.out.println("Erreur lors de la suppression Firebase: "+ex.getMessage());
                // erreur Firebase -> fallback local
            }

            // suppression locale
            Optional<Signalement> optSignalement = signalementRepository.findById(id);
            if (optSignalement.isEmpty() || optSignalement.get().getDateSuppression() != null) {
                return ApiResponse.error("Signalement non trouve", "LOCAL");
            }

            Signalement signalement = optSignalement.get();
            signalement.setDateSuppression(Instant.now());
            signalement.setDateMisAJour(Instant.now());
            signalementRepository.save(signalement);

            try {
                // tenter suppression en ligne
                firestoreService.deleteSignalement(id);
            } catch (Exception ignored) {}

            journalService.logDeleteSignalement(id);

            return ApiResponse.success(null, "Signalement supprime", "LOCAL");
        } catch (Exception e) {
            return ApiResponse.error("Erreur lors de la suppression: " + e.getMessage(), "LOCAL");
        }
    }

    // helper to get lat/lng from DB for a given signalement id
    private Double[] getLatLngFor(Integer id) {
        if (id == null) return new Double[]{null, null};
        String sql = "SELECT ST_Y(points) as lat, ST_X(points) as lng FROM signalements WHERE id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double lat = rs.getDouble("lat");
                    if (rs.wasNull()) return new Double[]{null, null};
                    double lng = rs.getDouble("lng");
                    if (rs.wasNull()) return new Double[]{null, null};
                    return new Double[]{lat, lng};
                }
            }
        } catch (SQLException e) {
            // ignore
        }
        return new Double[]{null, null};
    }

    private void updatePointsInDb(Integer id, Double longitude, Double latitude) {
        if (id == null || longitude == null || latitude == null) return;
        String sql = "UPDATE signalements SET points = ST_SetSRID(ST_MakePoint(?, ?), 4326) WHERE id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, longitude);
            ps.setDouble(2, latitude);
            ps.setInt(3, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            // log and ignore
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
        Object idObj = data.get("id");
        if (idObj == null) idObj = data.get("postgres_id");
        Integer id = ConversionUtils.toInteger(idObj);
        String firebaseId = (String) data.get("firebase_id");

        BigDecimal surface = ConversionUtils.toBigDecimal(data.get("surfaceM2"));
        BigDecimal budget = ConversionUtils.toBigDecimal(data.get("budget"));
        Integer idEntreprise = ConversionUtils.toInteger(data.get("idEntreprise"));
        Integer version = ConversionUtils.toInteger(data.get("version"));

        Instant dateCreation = ConversionUtils.toInstant(data.get("dateCreation"));
        Object majObj = data.get("dateMiseAJour");
        if (majObj == null) majObj = data.get("dateMisAJour");
        if (majObj == null) majObj = data.get("dateMiseAjour");
        Instant dateMisAJour = ConversionUtils.toInstant(majObj);

        Double latitude = null;
        Double longitude = null;
        Object locObj = data.get("location");
        if (locObj instanceof Map) {
            Map<?, ?> loc = (Map<?, ?>) locObj;
            Object latObj = loc.get("lat");
            Object lngObj = loc.get("lng");
            if (latObj instanceof Number) latitude = ((Number) latObj).doubleValue();
            else if (latObj instanceof String) {
                try { latitude = Double.parseDouble(((String) latObj).trim()); } catch (Exception ignored) {}
            }
            if (lngObj instanceof Number) longitude = ((Number) lngObj).doubleValue();
            else if (lngObj instanceof String) {
                try { longitude = Double.parseDouble(((String) lngObj).trim()); } catch (Exception ignored) {}
            }
        }

        return SignalementDTO.builder()
                .id(id)
                .firebaseId(firebaseId)
                .description((String) data.get("description"))
                .surfaceM2(surface)
                .budget(budget)
                .idEntreprise(idEntreprise)
                .version(version)
                .dateCreation(dateCreation)
                .dateMisAJour(dateMisAJour)
                .latitude(latitude)
                .longitude(longitude)
                .build();
    }
}

