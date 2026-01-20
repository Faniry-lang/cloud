package itu.cloud.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import itu.cloud.entities.Journal;
import itu.cloud.repositories.JournalRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Service de journalisation pour tracer toutes les operations.
 * Essentiel pour la synchronisation future avec Firebase.
 */
@Service
public class JournalService {

    private final JournalRepository journalRepository;
    private final ObjectMapper objectMapper;

    public JournalService(JournalRepository journalRepository, ObjectMapper objectMapper) {
        this.journalRepository = journalRepository;
        this.objectMapper = objectMapper;
    }

    public enum Operation {
        INSERT, UPDATE, DELETE
    }

    public enum TypeEntite {
        utilisateurs,
        roles_utilisateur,
        statuts_utilisateur,
        roles,
        statuts,
        journal,
        parametres,
        signalements,
        entreprises
    }

    /**
     * Enregistre une entree dans le journal
     */
    public void enregistrer(Integer idEntite, TypeEntite typeEntite, Operation operation,
                                Map<String, Object> donnees, Integer version) {
        Journal journal = new Journal();
        journal.setIdEntite(idEntite);
        journal.setTypeEntite(typeEntite.name());
        journal.setOperation(operation.name());
        journal.setDonnees(toJson(donnees));
        journal.setVersion(version);
        journal.setDateCreation(Instant.now());
        journal.setSynchronise(false);

        journalRepository.save(journal);
    }

    /**
     * Convertit une Map en JSON String
     */
    private String toJson(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    /**
     * Journalise une creation d'utilisateur
     */
    public void logCreationUtilisateur(Integer userId, String email, String nom) {
        Map<String, Object> donnees = new HashMap<>();
        donnees.put("action", "REGISTER");
        donnees.put("email", email);
        donnees.put("nom", nom != null ? nom : "");
        donnees.put("timestamp", Instant.now().toString());
        enregistrer(userId, TypeEntite.utilisateurs, Operation.INSERT, donnees, 1);
    }

    /**
     * Journalise une connexion reussie
     */
    public void logConnexionReussie(Integer userId, String email, String authMode) {
        Map<String, Object> donnees = new HashMap<>();
        donnees.put("action", "LOGIN_SUCCESS");
        donnees.put("email", email);
        donnees.put("authMode", authMode);
        donnees.put("timestamp", Instant.now().toString());
        enregistrer(userId, TypeEntite.utilisateurs, Operation.UPDATE, donnees, null);
    }

    /**
     * Journalise une tentative de connexion echouee
     */
    public void logConnexionEchouee(String email, String raison) {
        Map<String, Object> donnees = new HashMap<>();
        donnees.put("action", "LOGIN_FAILED");
        donnees.put("email", email);
        donnees.put("raison", raison);
        donnees.put("timestamp", Instant.now().toString());
        enregistrer(null, TypeEntite.utilisateurs, Operation.UPDATE, donnees, null);
    }

    /**
     * Journalise un blocage de compte
     */
    public void logBlocageCompte(Integer userId, String email, Instant bloqueJusqua) {
        Map<String, Object> donnees = new HashMap<>();
        donnees.put("action", "ACCOUNT_BLOCKED");
        donnees.put("email", email);
        donnees.put("bloqueJusqua", bloqueJusqua.toString());
        donnees.put("timestamp", Instant.now().toString());
        enregistrer(userId, TypeEntite.utilisateurs, Operation.UPDATE, donnees, null);
    }

    /**
     * Journalise un deblocage de compte
     */
    public void logDeblocageCompte(Integer userId, String email) {
        Map<String, Object> donnees = new HashMap<>();
        donnees.put("action", "ACCOUNT_UNBLOCKED");
        donnees.put("email", email);
        donnees.put("timestamp", Instant.now().toString());
        enregistrer(userId, TypeEntite.utilisateurs, Operation.UPDATE, donnees, null);
    }

    /**
     * Journalise l'attribution d'un role
     */
    public void logAttributionRole(Integer userId, String roleNom) {
        Map<String, Object> donnees = new HashMap<>();
        donnees.put("action", "ROLE_ASSIGNED");
        donnees.put("role", roleNom);
        donnees.put("timestamp", Instant.now().toString());
        enregistrer(userId, TypeEntite.roles_utilisateur, Operation.INSERT, donnees, null);
    }

    /**
     * Journalise l'attribution d'un statut
     */
    public void logAttributionStatut(Integer userId, String statutDescription) {
        Map<String, Object> donnees = new HashMap<>();
        donnees.put("action", "STATUS_ASSIGNED");
        donnees.put("statut", statutDescription);
        donnees.put("timestamp", Instant.now().toString());
        enregistrer(userId, TypeEntite.statuts_utilisateur, Operation.INSERT, donnees, null);
    }

    // ==================== ENTREPRISES ====================

    /**
     * Journalise la creation d'une entreprise
     */
    public void logCreationEntreprise(Integer entrepriseId, String nom) {
        Map<String, Object> donnees = new HashMap<>();
        donnees.put("action", "CREATE");
        donnees.put("nom", nom);
        donnees.put("timestamp", Instant.now().toString());
        enregistrer(entrepriseId, TypeEntite.entreprises, Operation.INSERT, donnees, 1);
    }

    /**
     * Journalise la mise a jour d'une entreprise
     */
    public void logUpdateEntreprise(Integer entrepriseId, String nom, Integer version) {
        Map<String, Object> donnees = new HashMap<>();
        donnees.put("action", "UPDATE");
        donnees.put("nom", nom);
        donnees.put("timestamp", Instant.now().toString());
        enregistrer(entrepriseId, TypeEntite.entreprises, Operation.UPDATE, donnees, version);
    }

    /**
     * Journalise la suppression d'une entreprise
     */
    public void logDeleteEntreprise(Integer entrepriseId, String nom) {
        Map<String, Object> donnees = new HashMap<>();
        donnees.put("action", "DELETE");
        donnees.put("nom", nom);
        donnees.put("timestamp", Instant.now().toString());
        enregistrer(entrepriseId, TypeEntite.entreprises, Operation.DELETE, donnees, null);
    }

    // ==================== SIGNALEMENTS ====================

    /**
     * Journalise la creation d'un signalement
     */
    public void logCreationSignalement(Integer signalementId, String description, Integer idEntreprise) {
        Map<String, Object> donnees = new HashMap<>();
        donnees.put("action", "CREATE");
        donnees.put("description", description);
        donnees.put("idEntreprise", idEntreprise);
        donnees.put("timestamp", Instant.now().toString());
        enregistrer(signalementId, TypeEntite.signalements, Operation.INSERT, donnees, 1);
    }

    /**
     * Journalise la mise a jour d'un signalement
     */
    public void logUpdateSignalement(Integer signalementId, String description, Integer version) {
        Map<String, Object> donnees = new HashMap<>();
        donnees.put("action", "UPDATE");
        donnees.put("description", description);
        donnees.put("timestamp", Instant.now().toString());
        enregistrer(signalementId, TypeEntite.signalements, Operation.UPDATE, donnees, version);
    }

    /**
     * Journalise la suppression d'un signalement
     */
    public void logDeleteSignalement(Integer signalementId) {
        Map<String, Object> donnees = new HashMap<>();
        donnees.put("action", "DELETE");
        donnees.put("timestamp", Instant.now().toString());
        enregistrer(signalementId, TypeEntite.signalements, Operation.DELETE, donnees, null);
    }

    // ==================== ROLES ====================

    /**
     * Journalise la creation d'un role
     */
    public void logCreationRole(Integer roleId, String nom) {
        Map<String, Object> donnees = new HashMap<>();
        donnees.put("action", "CREATE");
        donnees.put("nom", nom);
        donnees.put("timestamp", Instant.now().toString());
        enregistrer(roleId, TypeEntite.roles, Operation.INSERT, donnees, 1);
    }

    /**
     * Journalise la mise a jour d'un role
     */
    public void logUpdateRole(Integer roleId, String nom) {
        Map<String, Object> donnees = new HashMap<>();
        donnees.put("action", "UPDATE");
        donnees.put("nom", nom);
        donnees.put("timestamp", Instant.now().toString());
        enregistrer(roleId, TypeEntite.roles, Operation.UPDATE, donnees, null);
    }

    /**
     * Journalise la suppression d'un role
     */
    public void logDeleteRole(Integer roleId, String nom) {
        Map<String, Object> donnees = new HashMap<>();
        donnees.put("action", "DELETE");
        donnees.put("nom", nom);
        donnees.put("timestamp", Instant.now().toString());
        enregistrer(roleId, TypeEntite.roles, Operation.DELETE, donnees, null);
    }

    // ==================== STATUTS ====================

    /**
     * Journalise la creation d'un statut
     */
    public void logCreationStatut(Integer statutId, String description) {
        Map<String, Object> donnees = new HashMap<>();
        donnees.put("action", "CREATE");
        donnees.put("description", description);
        donnees.put("timestamp", Instant.now().toString());
        enregistrer(statutId, TypeEntite.statuts, Operation.INSERT, donnees, 1);
    }

    /**
     * Journalise la mise a jour d'un statut
     */
    public void logUpdateStatut(Integer statutId, String description) {
        Map<String, Object> donnees = new HashMap<>();
        donnees.put("action", "UPDATE");
        donnees.put("description", description);
        donnees.put("timestamp", Instant.now().toString());
        enregistrer(statutId, TypeEntite.statuts, Operation.UPDATE, donnees, null);
    }

    /**
     * Journalise la suppression d'un statut
     */
    public void logDeleteStatut(Integer statutId, String description) {
        Map<String, Object> donnees = new HashMap<>();
        donnees.put("action", "DELETE");
        donnees.put("description", description);
        donnees.put("timestamp", Instant.now().toString());
        enregistrer(statutId, TypeEntite.statuts, Operation.DELETE, donnees, null);
    }
}

