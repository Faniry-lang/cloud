package itu.cloud.controllers;

import itu.cloud.collections.UtilisateurCollection;
import itu.cloud.entities.Utilisateur;
import itu.cloud.firebase.services.UtilisateurFirebaseService;
import itu.cloud.repositories.UtilisateurRepository;
import itu.cloud.service.JournalService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/manager/user")
public class UtilisateurController {

    private final UtilisateurFirebaseService utilisateurFirebaseService;
    private final UtilisateurRepository utilisateurRepository;
    private final JournalService journalService;

    public UtilisateurController(UtilisateurFirebaseService utilisateurFirebaseService,
                                UtilisateurRepository utilisateurRepository,
                                JournalService journalService) {
        this.utilisateurFirebaseService = utilisateurFirebaseService;
        this.utilisateurRepository = utilisateurRepository;
        this.journalService = journalService;
    }

    @GetMapping("/all")
    public List<UtilisateurCollection> getAllUtilisateurs() {
        List<Utilisateur> utilisateurs = utilisateurRepository.findAll();
        List<UtilisateurCollection> utilisateurCollections = new ArrayList<>();

        for (Utilisateur utilisateur : utilisateurs) {
            utilisateurCollections.add(convertToCollection(utilisateur));
        }

        return utilisateurCollections;
    }

    @GetMapping("/blocked")
    public ResponseEntity<?> getAllBlockedUsers() {
        try {
            List<Utilisateur> blockedUsers = utilisateurRepository.findBlockedUsers(LocalDateTime.now());
            List<UtilisateurCollection> utilisateurCollections = new ArrayList<>();

            for (Utilisateur utilisateur : blockedUsers) {
                utilisateurCollections.add(convertToCollection(utilisateur));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", utilisateurCollections.size());
            response.put("data", utilisateurCollections);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors de la récupération des utilisateurs bloqués: " + e.getMessage()));
        }
    }

    @PostMapping("/unblock/{userId}")
    public ResponseEntity<?> unblockUser(@PathVariable Integer userId) {
        try {
            if (userId == null) {
                return ResponseEntity
                        .badRequest()
                        .body(createErrorResponse("L'ID de l'utilisateur est requis"));
            }

            Utilisateur utilisateur = utilisateurRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            utilisateur.setTentativesEchouees(0);
            utilisateur.setBloqueJusqua(null);
            utilisateur.setActif(true);
            utilisateur.setDateMisAJour(LocalDateTime.now());

            Utilisateur updatedUser = utilisateurRepository.save(utilisateur);

            UtilisateurCollection utilisateurCollection = convertToCollection(updatedUser);
            journalService.journaliser(
                "UtilisateurCollection",
                "UPDATE",
                updatedUser.getDocId(),
                utilisateurCollection,
                updatedUser.getVersion()
            );

            return ResponseEntity.ok(createSuccessResponse(utilisateurCollection, "Utilisateur débloqué avec succès"));

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors du déblocage: " + e.getMessage()));
        }
    }

    private UtilisateurCollection convertToCollection(Utilisateur utilisateur) {
        UtilisateurCollection collection = new UtilisateurCollection();
        collection.setEmail(utilisateur.getEmail());
        collection.setNom(utilisateur.getNom());
        collection.setFirebaseUid(utilisateur.getFirebaseUid());
        collection.setActif(utilisateur.getActif() != null ? utilisateur.getActif() : false);
        collection.setTentativesEchouees(utilisateur.getTentativesEchouees() != null ? utilisateur.getTentativesEchouees() : 0);
        collection.setVersion(utilisateur.getVersion());
        collection.setId(utilisateur.getId());
        collection.setSynchronise(false);

        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
        if (utilisateur.getBloqueJusqua() != null) {
            collection.setBloqueJusqua(utilisateur.getBloqueJusqua().format(formatter));
        }
        if (utilisateur.getDateCreation() != null) {
            collection.setDateCreation(utilisateur.getDateCreation().format(formatter));
        }
        if (utilisateur.getDateMisAJour() != null) {
            collection.setDateMiseAJour(utilisateur.getDateMisAJour().format(formatter));
        }

        return collection;
    }

    private Map<String, Object> createSuccessResponse(UtilisateurCollection utilisateur, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("data", utilisateur);
        return response;
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        return response;
    }

}
