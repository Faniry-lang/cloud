package itu.cloud.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app")
public class AuthProperties {

    public enum AppMode {
        ONLINE,
        OFFLINE,
        AUTO
    }

    private AppMode mode = AppMode.AUTO;
}

