package itu.cloud.service;

import itu.cloud.entities.Parametre;
import itu.cloud.repositories.ParametreRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

/**
 * Service de gestion des paramètres applicatifs stockés en base de données.
 * Permet de centraliser la configuration et de la modifier sans redéploiement.
 */
@Service
public class ParametreService {

    // Noms des paramètres de sécurité
    public static final String PARAM_MAX_FAILED_ATTEMPTS = "MAX_FAILED_ATTEMPTS";
    public static final String PARAM_BLOCK_DURATION_MINUTES = "BLOCK_DURATION_MINUTES";
    public static final String PARAM_DEFAULT_ROLE = "DEFAULT_ROLE";
    public static final String PARAM_DEFAULT_STATUS = "DEFAULT_STATUS";

    // Valeurs par défaut (fallback si non présent en BDD)
    private static final int DEFAULT_MAX_FAILED_ATTEMPTS = 5;
    private static final int DEFAULT_BLOCK_DURATION_MINUTES = 30;
    private static final String DEFAULT_ROLE = "USER";
    private static final String DEFAULT_STATUS = "ACTIF";

    private final ParametreRepository parametreRepository;

    public ParametreService(ParametreRepository parametreRepository) {
        this.parametreRepository = parametreRepository;
    }

    public Optional<Parametre> getParametre(String nom) {
        return parametreRepository.findByNomAndDateSuppressionIsNull(nom);
    }

    public String getString(String nom, String defaultValue) {
        return getParametre(nom)
                .map(Parametre::getValeur)
                .orElse(defaultValue);
    }

    public int getInt(String nom, int defaultValue) {
        return getParametre(nom)
                .map(p -> {
                    try {
                        return Integer.parseInt(p.getValeur());
                    } catch (NumberFormatException e) {
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }

    public boolean getBoolean(String nom, boolean defaultValue) {
        return getParametre(nom)
                .map(p -> Boolean.parseBoolean(p.getValeur()))
                .orElse(defaultValue);
    }
    public int getMaxFailedAttempts() {
        return getInt(PARAM_MAX_FAILED_ATTEMPTS, DEFAULT_MAX_FAILED_ATTEMPTS);
    }

    public int getBlockDurationMinutes() {
        return getInt(PARAM_BLOCK_DURATION_MINUTES, DEFAULT_BLOCK_DURATION_MINUTES);
    }

    public String getDefaultRole() {
        return getString(PARAM_DEFAULT_ROLE, DEFAULT_ROLE);
    }

    public String getDefaultStatus() {
        return getString(PARAM_DEFAULT_STATUS, DEFAULT_STATUS);
    }

    public Parametre setParametre(String nom, String valeur, String type) {
        Optional<Parametre> existant = getParametre(nom);

        Parametre parametre;
        if (existant.isPresent()) {
            parametre = existant.get();
            parametre.setValeur(valeur);
            parametre.setType(type);
            parametre.setDateMisAJour(Instant.now());
        } else {
            parametre = new Parametre();
            parametre.setNom(nom);
            parametre.setValeur(valeur);
            parametre.setType(type);
            parametre.setDateCreation(Instant.now());
        }

        return parametreRepository.save(parametre);
    }

    public void initialiserParametresParDefaut() {
        if (getParametre(PARAM_MAX_FAILED_ATTEMPTS).isEmpty()) {
            setParametre(PARAM_MAX_FAILED_ATTEMPTS, String.valueOf(DEFAULT_MAX_FAILED_ATTEMPTS), "INTEGER");
        }
        if (getParametre(PARAM_BLOCK_DURATION_MINUTES).isEmpty()) {
            setParametre(PARAM_BLOCK_DURATION_MINUTES, String.valueOf(DEFAULT_BLOCK_DURATION_MINUTES), "INTEGER");
        }
        if (getParametre(PARAM_DEFAULT_ROLE).isEmpty()) {
            setParametre(PARAM_DEFAULT_ROLE, DEFAULT_ROLE, "STRING");
        }
        if (getParametre(PARAM_DEFAULT_STATUS).isEmpty()) {
            setParametre(PARAM_DEFAULT_STATUS, DEFAULT_STATUS, "STRING");
        }
    }
}

