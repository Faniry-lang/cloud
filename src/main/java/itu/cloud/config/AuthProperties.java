package itu.cloud.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration du mode de fonctionnement de l'application.
 * ONLINE: Firebase/Firestore par defaut
 * OFFLINE: PostgreSQL local par defaut
 * AUTO: Detecte automatiquement la connexion Firebase
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app")
public class AuthProperties {

    public enum AppMode {
        ONLINE,   // Firebase par defaut
        OFFLINE,  // Local par defaut
        AUTO      // Detection automatique
    }

    private AppMode mode = AppMode.AUTO;
}

