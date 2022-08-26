package de.bermuda.hero.backend;

import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cloud.context.scope.refresh.RefreshScope;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.vault.VaultException;
import org.springframework.vault.core.lease.SecretLeaseContainer;
import org.springframework.vault.core.lease.domain.RequestedSecret;
import org.springframework.vault.core.lease.event.LeaseListener;
import org.springframework.vault.core.lease.event.SecretLeaseCreatedEvent;
import org.springframework.vault.core.lease.event.SecretLeaseEvent;
import org.springframework.vault.core.lease.event.SecretLeaseExpiredEvent;

@ConditionalOnBean(SecretLeaseContainer.class)
@Configuration
public class VaultSecretRotationConfiguration {
    public static final String VAULT_DB_CREDENTIALS_PATH_PREFIX = "database/creds/";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    private static final Logger LOGGER = LoggerFactory.getLogger(VaultSecretRotationConfiguration.class);
    private final SecretLeaseContainer leaseContainer;
    private final String databaseRole;
    private final ConfigurableApplicationContext applicationContext;
    private final RefreshScope refreshScope;
    private final String usernameProperty;
    private final String passwordProperty;

    public VaultSecretRotationConfiguration(SecretLeaseContainer leaseContainer,
                                            ConfigurableApplicationContext applicationContext,
                                            RefreshScope refreshScope,
                                            @Value("${spring.cloud.vault.database.role}") String databaseRole,
                                            @Value("${spring.cloud.vault.database.username-property}") String usernameProperty,
                                            @Value("${spring.cloud.vault.database.password-property}") String passwordProperty) {
        this.leaseContainer = leaseContainer;
        this.databaseRole = databaseRole;
        this.applicationContext = applicationContext;
        this.refreshScope = refreshScope;
        this.usernameProperty = usernameProperty;
        this.passwordProperty = passwordProperty;
    }

    protected void refreshDatabaseConnection(Credentials credentials) {
        updateDBProperties(credentials);
        updateDatasource(credentials);
    }

    protected void updateDBProperties(Credentials credentials) {
        LOGGER.info("Setting {} to {}", usernameProperty, credentials.username);
        System.setProperty(usernameProperty, credentials.username);
        System.setProperty(passwordProperty, credentials.password);
    }

    protected void updateDatasource(Credentials credentials) {
        LOGGER.info("Update database credentials");
        refreshScope.refreshAll();
    }

    @PostConstruct
    private void postConstruct() {
        leaseContainer.addLeaseListener(new RotatingLeaseListener());
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

    class RotatingLeaseListener implements LeaseListener {
        @Override
        public void onLeaseEvent(SecretLeaseEvent secretLeaseEvent) {
            var vaultCredsPath = VAULT_DB_CREDENTIALS_PATH_PREFIX + databaseRole;
            try {
                if (secretLeaseEvent.getSource().getPath().equals(vaultCredsPath)) {
                    LOGGER.info("Lease Change Event for DB: {}: {}", secretLeaseEvent, secretLeaseEvent.getLease());
                    if (secretLeaseEvent instanceof SecretLeaseExpiredEvent
                            && secretLeaseEvent.getSource().getMode() == RequestedSecret.Mode.RENEW) {
                        LOGGER.info("Replace RENEW for expired credential with ROTATE");
                        leaseContainer.requestRotatingSecret(vaultCredsPath);
                    } else if (secretLeaseEvent instanceof SecretLeaseCreatedEvent secretLeaseCreatedEvent
                            && secretLeaseEvent.getSource().getMode() == RequestedSecret.Mode.ROTATE) {
                        var credentials = retrieveCredentials(secretLeaseCreatedEvent.getSecrets());
                        if (credentials == null) {
                            LOGGER.error("Cannot get updated DB credentials. Shutting down.");
                            applicationContext.close();
                            return;
                        }
                        refreshDatabaseConnection(credentials);
                    }
                }
            } catch (VaultException ve) {
                LOGGER.warn("Vault Exception", ve);
            }
        }
    }

    private record Credentials(String username, String password) {
    }

}
