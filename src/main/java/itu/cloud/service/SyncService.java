package itu.cloud.service;

import itu.cloud.collections.SignalementCollection;
import itu.cloud.collections.TypeSignalementCollection;
import itu.cloud.entities.Journal;
import itu.cloud.entities.Signalement;
import itu.cloud.collections.*;
import itu.cloud.entities.TypeSignalement;
import itu.cloud.firebase.services.SignalementFirebaseService;
import itu.cloud.firebase.services.TypeSignalementFirebaseService;
import itu.cloud.firebase.services.UtilisateurFirebaseService;
import itu.cloud.repositories.JournalRepository;
import itu.cloud.repositories.SignalementRepository;
import itu.cloud.repositories.TypeSignalementRepository;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class SyncService {

    private final SignalementFirebaseService signalementFirebaseService;
    private final UtilisateurFirebaseService utilisateurFirebaseService;
    private final TypeSignalementFirebaseService typeSignalementFirebaseService;
    private final SignalementRepository signalementRepository;
    private final TypeSignalementRepository typeSignalementRepository;
    private final JournalRepository journalRepository;
    private final JournalService journalService;
    private final GeometryFactory geometryFactory;
    private final UtilisateurService utilisateurService;

    public SyncService(SignalementFirebaseService signalementFirebaseService,
                       UtilisateurFirebaseService utilisateurFirebaseService,
                       TypeSignalementFirebaseService typeSignalementFirebaseService,
                       SignalementRepository signalementRepository,
                       TypeSignalementRepository typeSignalementRepository,
                       JournalRepository journalRepository,
                       JournalService journalService, UtilisateurService utilisateurService) {
        this.signalementFirebaseService = signalementFirebaseService;
        this.utilisateurFirebaseService = utilisateurFirebaseService;
        this.typeSignalementFirebaseService = typeSignalementFirebaseService;
        this.signalementRepository = signalementRepository;
        this.typeSignalementRepository = typeSignalementRepository;
        this.journalRepository = journalRepository;
        this.journalService = journalService;
        this.utilisateurService = utilisateurService;
        this.geometryFactory = new GeometryFactory();
    }

    public int pull() throws Exception {
        List<SignalementCollection> signalements = signalementFirebaseService.find(null);
        int count = 0;

        for (SignalementCollection signalementCollection : signalements) {
            if (signalementCollection.getPostgres_id() == null) {
                Signalement signalement = new Signalement();
                signalement.setFirebaseUid(signalementCollection.getDocId());
                signalement.setDescription(signalementCollection.getDescription());
                signalement.setSurfaceM2(signalementCollection.getSurfaceM2() != null ? signalementCollection.getSurfaceM2() : 0);
                signalement.setBudget(signalementCollection.getBudget() != null ? signalementCollection.getBudget() : 0);
                signalement.setVersion(signalementCollection.getVersion());
                signalement.setDateCreation(LocalDateTime.now());

                if (signalementCollection.getIdTypeSignalement() != null) {
                    String typeSignalementId = signalementCollection.getIdTypeSignalement().getId();

                    List<TypeSignalementCollection> typeCollections = typeSignalementFirebaseService.find(typeSignalementId);

                    if (!typeCollections.isEmpty()) {
                        TypeSignalementCollection typeCollection = typeCollections.get(0);

                        TypeSignalement typeSignalement = null;

                        Optional<TypeSignalement> existingByFirebaseUid = typeSignalementRepository.findByFirebaseUid(typeCollection.getDocId());

                        if (existingByFirebaseUid.isPresent()) {
                            typeSignalement = existingByFirebaseUid.get();

                            if (typeCollection.getPostgres_id() == null || !typeCollection.getPostgres_id().equals(typeSignalement.getId())) {
                                typeCollection.setPostgres_id(typeSignalement.getId());
                                typeSignalementFirebaseService.update(typeCollection.getDocId(), typeCollection);
                            }
                        } else if (typeCollection.getPostgres_id() != null) {
                            Optional<TypeSignalement> existingById = typeSignalementRepository.findById(typeCollection.getPostgres_id());
                            if (existingById.isPresent()) {
                                typeSignalement = existingById.get();
                                if (typeSignalement.getFirebaseUid() == null) {
                                    typeSignalement.setFirebaseUid(typeCollection.getDocId());
                                    typeSignalement = typeSignalementRepository.save(typeSignalement);
                                }
                            }
                        }

                        if (typeSignalement == null) {
                            typeSignalement = new TypeSignalement();
                            typeSignalement.setFirebaseUid(typeCollection.getDocId());
                            typeSignalement.setNom(typeCollection.getNom());
                            typeSignalement.setIcone(typeCollection.getIcone());
                            typeSignalement = typeSignalementRepository.save(typeSignalement);

                            typeCollection.setPostgres_id(typeSignalement.getId());
                            typeSignalementFirebaseService.update(typeCollection.getDocId(), typeCollection);
                        }

                        signalement.setIdTypeSignalement(typeSignalement);
                    }
                }

                if (signalementCollection.getLocation() != null) {
                    SignalementCollection.Location location = signalementCollection.getLocation();
                    if (location.getLat() != null && location.getLng() != null) {
                        Point point = geometryFactory.createPoint(
                            new Coordinate(location.getLng(), location.getLat())
                        );
                        point.setSRID(4326);
                        signalement.setPoints(point);
                    }
                }

                Signalement savedSignalement = signalementRepository.save(signalement);
                signalementCollection.setPostgres_id(savedSignalement.getId());
                signalementFirebaseService.update(signalementCollection.getDocId(), signalementCollection);

                count++;
            }
        }

        return count;
    }

    public int push() throws Exception {
        List<Journal> journals = journalRepository.findBySynchroniseFalse();
        int count = 0;

        for (Journal journal : journals) {
            try {
                switch (journal.getTypeEntite()) {
                    case "UtilisateurCollection":
                        UtilisateurCollection uc = utilisateurFirebaseService.saveFromJournal(journal);
                        this.utilisateurService.updateFirebaseIds(uc);
                        break;

                    case "SignalementCollection":
                        signalementFirebaseService.saveFromJournal(journal);
                        break;

                    default:
                        System.out.println("Type d'entité non supporté: " + journal.getTypeEntite());
                        continue;
                }

                journalService.marquerCommeSynchronise(journal.getId());
                count++;

            } catch (Exception e) {
                System.err.println("Erreur lors de la synchronisation du journal " + journal.getId() + ": " + e.getMessage());
            }
        }

        return count;
    }
}
