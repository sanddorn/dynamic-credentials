package de.bermuda.hero.frontend;

import java.util.Map;

import javax.annotation.PostConstruct;

import de.bermuda.hero.client.ApiClient;
import de.bermuda.hero.client.api.HeroApi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.vault.core.lease.SecretLeaseContainer;
import org.springframework.vault.core.lease.domain.RequestedSecret;
import org.springframework.vault.core.lease.event.SecretLeaseCreatedEvent;
import org.springframework.vault.core.lease.event.SecretLeaseExpiredEvent;

@Configuration
public class VaultSecretRotationConfiguration {
    public static final String VAULT_AUTH_CREDENTIALS_PATH = "backenduser/creds/hero";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String USERNAME_PROPERTY = "rest.username";
    public static final String PASSWORD_PROPERTY = "rest.password";

    private static final Logger LOGGER = LoggerFactory.getLogger(VaultSecretRotationConfiguration.class);
    private final SecretLeaseContainer leaseContainer;
    private final ConfigurableApplicationContext applicationContext;
    private final org.springframework.cloud.context.scope.refresh.RefreshScope refreshScope;

    public VaultSecretRotationConfiguration(SecretLeaseContainer leaseContainer,
                                            ConfigurableApplicationContext applicationContext,
                                            org.springframework.cloud.context.scope.refresh.RefreshScope refreshScope) {
        this.leaseContainer = leaseContainer;
        this.applicationContext = applicationContext;
        this.refreshScope = refreshScope;
    }

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

    @PostConstruct
    private void postConstruct() {
        leaseContainer.addLeaseListener(secretLeaseEvent -> {
            if (secretLeaseEvent.getSource().getPath().equals(VAULT_AUTH_CREDENTIALS_PATH)) {
                LOGGER.info("Lease Change Event for DB: {}: {}", secretLeaseEvent, secretLeaseEvent.getLease());
                if (secretLeaseEvent instanceof SecretLeaseExpiredEvent
                            && secretLeaseEvent.getSource().getMode() == RequestedSecret.Mode.RENEW) {
                    LOGGER.info("Replace RENEW for expired credential with ROTATE");
                    leaseContainer.requestRotatingSecret(VAULT_AUTH_CREDENTIALS_PATH);
                } else if (secretLeaseEvent instanceof SecretLeaseCreatedEvent secretLeaseCreatedEvent
                                   && secretLeaseEvent.getSource().getMode() == RequestedSecret.Mode.ROTATE) {
                    var credentials = retrieveCredentials(secretLeaseCreatedEvent.getSecrets());
                    if (credentials == null) {
                        LOGGER.error("Cannot get updated DB credentials. Shutting down.");
                        applicationContext.close();
                        return;
                    }
                    refreshHttpConnection(credentials);
                }
            }
        });
    }

    protected void refreshHttpConnection(Credentials credentials) {
        updateHttpAuthProperties(credentials);
        updateDatasource(credentials);
    }

    protected void updateHttpAuthProperties(Credentials credentials) {
        LOGGER.info("Setting {} to {}", USERNAME_PROPERTY, credentials.username);
        System.setProperty(USERNAME_PROPERTY, credentials.username);
        System.setProperty(PASSWORD_PROPERTY, credentials.password);
    }

    protected void updateDatasource(Credentials credentials) {
        LOGGER.info("Update database credentials");
        refreshScope.refreshAll();
    }


    private Credentials retrieveCredentials(Map<String, Object> leasedCredentials) {
        if (leasedCredentials.get(USERNAME) == null) {
            return null;
        }
        if (leasedCredentials.get(PASSWORD) == null) {
            return null;
        }
        return new Credentials((String) leasedCredentials.get(USERNAME), (String) leasedCredentials.get(PASSWORD));
    }

    private record Credentials(String username, String password) {
    }

}
