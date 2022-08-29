package de.bermuda.hero.frontend;

import java.util.HashMap;
import java.util.Map;

import org.springframework.cloud.vault.config.LeasingSecretBackendMetadata;
import org.springframework.cloud.vault.config.PropertyNameTransformer;
import org.springframework.cloud.vault.config.SecretBackendConfigurer;
import org.springframework.cloud.vault.config.VaultConfigurer;
import org.springframework.vault.core.lease.domain.RequestedSecret;
import org.springframework.vault.core.util.PropertyTransformer;
import org.springframework.vault.core.util.PropertyTransformers;


public class FrontendVaultConfigurer implements VaultConfigurer {


    @Override
    public void addSecretBackends(SecretBackendConfigurer configurer) {
        System.out.println("FrontendVaultConfigurer");
        configurer.add(new AuthorizationBackendMetadata());
        configurer.registerDefaultKeyValueSecretBackends(true);
        configurer.registerDefaultDiscoveredSecretBackends(true);
    }

    public class AuthorizationBackendMetadata implements LeasingSecretBackendMetadata {

        private final PropertyTransformer propertyTransformer;

        public AuthorizationBackendMetadata() {
            var propertyNameTransformer = new PropertyNameTransformer();
            propertyNameTransformer.addKeyTransformation("username", "rest.username");
            propertyNameTransformer.addKeyTransformation("password", "rest.password");
            propertyTransformer = propertyNameTransformer;
        }

        /**
         * Return the lease mode of this secret backend.
         * <p>
         * Lease mode is considered only by lease-aware property sources.
         *
         * @return the lease mode of this secret backend.
         * @since 1.1
         */


        @Override
        public RequestedSecret.Mode getLeaseMode() {
            return RequestedSecret.Mode.ROTATE;
        }

        /**
         * Return a readable name of this secret backend.
         *
         * @return the name of this secret backend.
         */
        @Override
        public String getName() {
            return "AuthorizationBackendMetadata";
        }

        /**
         * Return the path of this secret backend.
         *
         * @return the path of this secret backend.
         * @since 1.1
         */
        @Override
        public String getPath() {
            return "backenduser/creds/hero"; // Should be configurable!
        }

        /**
         * Return a {@link PropertyTransformer} to post-process properties retrieved from
         * Vault.
         *
         * @return the property transformer.
         * @see PropertyTransformers
         */
        @Override
        public PropertyTransformer getPropertyTransformer() {
            return propertyTransformer;
        }

        /**
         * @return the URL template variables. URI variables should declare either
         * {@code backend} and {@code key} or {@code path} properties.
         */
        @Override
        public Map<String, String> getVariables() {
            Map<String, String> variables = new HashMap<>();
            return variables;
        }
    }
}
