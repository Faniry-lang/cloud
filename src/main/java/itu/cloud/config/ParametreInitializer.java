package itu.cloud.config;

import itu.cloud.service.ParametreService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Initialise les paramètres par défaut au démarrage de l'application.
 * Les paramètres sont créés dans la table 'parametres' s'ils n'existent pas.
 */
@Component
public class ParametreInitializer implements CommandLineRunner {

    private final ParametreService parametreService;

    public ParametreInitializer(ParametreService parametreService) {
        this.parametreService = parametreService;
    }

    @Override
    public void run(String... args) {
        parametreService.initialiserParametresParDefaut();
        System.out.println("✓ Paramètres de sécurité initialisés depuis la base de données");
    }
}

