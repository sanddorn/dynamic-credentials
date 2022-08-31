package de.bermuda.hero.frontend;

import de.bermuda.hero.client.ApiClient;
import de.bermuda.hero.client.api.HeroApi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.vault.config.VaultBootstrapper;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class HeroApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(HeroApplication.class);

    @Bean
    @RefreshScope
    HeroApi createHeroApi(
            @Value("${backend.url}") String backendUrl,
            @Value("${rest.username}") String username,
            @Value("${rest.password}") String password) {
        var heroApi = new HeroApi();
        final ApiClient apiClient = heroApi.getApiClient();
        apiClient.setBasePath(backendUrl);

        if (username != null) {
            apiClient.setPassword(password);
            apiClient.setUsername(username);
        } else {
            LOGGER.info("No username, password: '{}'", password);
        }
        return heroApi;
    }

    public static void main(String[] args) {
        var application = new SpringApplication(HeroApplication.class);
        application.addBootstrapRegistryInitializer(VaultBootstrapper.fromConfigurer(new FrontendVaultConfigurer()));
        application.run(args);
    }


}
