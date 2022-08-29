package de.bermuda.hero.frontend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.vault.config.VaultBootstrapper;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class HeroApplication {

    public static void main(String[] args) {
        var application = new SpringApplication(HeroApplication.class);
        application.addBootstrapRegistryInitializer(VaultBootstrapper.fromConfigurer(new FrontendVaultConfigurer()));
        application.run(args);
    }
}
