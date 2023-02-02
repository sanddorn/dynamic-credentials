package de.bermuda.hero.frontend.universum;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.bermuda.hero.client.ApiClient;
import de.bermuda.hero.client.ApiException;
import de.bermuda.hero.client.ServerConfiguration;
import de.bermuda.hero.client.ServerVariable;
import de.bermuda.hero.client.api.HeroApi;
import de.bermuda.hero.client.model.Hero;

import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

@Component
public class HeroServiceRestClient implements HeroService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HeroServiceRestClient.class);
    private final HeroApi heroApi;

    public HeroServiceRestClient(@Value("${backend.url}") String backendUrl,
                                 @Value("${rest.username}") String username,
                                 @Value("${rest.password}") String password,
                                 @Value("${apiclient.trust-certificate}") String certificate) {
        heroApi = new HeroApi();
        final ApiClient apiClient = heroApi.getApiClient();

        var serverConfiguration = new ServerConfiguration(
                backendUrl,
                "MyServer",
                new HashMap<String, ServerVariable>()
        );
        apiClient.setServers(List.of(serverConfiguration));

        if (Strings.isNotBlank(certificate)) {
            LOGGER.info("Certificate: '{}", certificate);
            apiClient.setVerifyingSsl(true);
            var certificateStream = new ByteArrayInputStream(certificate.getBytes(StandardCharsets.UTF_8));
            apiClient.setSslCaCert(certificateStream);
        } else {
            apiClient.setVerifyingSsl(false);
        }


        if (username != null) {
            apiClient.setPassword(password);
            apiClient.setUsername(username);
        } else {
            LOGGER.info("No username, password: '{}'", password);
        }
    }

    @Override
    public List<Hero> collectAllHeros() {
        List<Hero> heroList = new ArrayList<>();
        try {
            heroList = heroApi.collectAllHeros();
        } catch (RestClientException | ApiException e) {
            LOGGER.info("Could not collect all Heros: Code {}", ((ApiException) e).getCode(),  e);
            if (e.getCause() instanceof CertificateException) {
                LOGGER.info("Certificate Prob", e.getCause());
            }
        }
        return heroList;
    }

    @Override
    public void addHeroToRepository(Hero hero) {
        try {
            heroApi.addHeroToRepository(hero);
        } catch (RestClientException | ApiException e) {
            LOGGER.debug("Could not add Hero", e);
        }
    }
}
