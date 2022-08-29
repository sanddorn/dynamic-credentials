package de.bermuda.hero.frontend.universum;

import java.util.ArrayList;
import java.util.List;

import de.bermuda.hero.client.ApiException;
import de.bermuda.hero.client.api.HeroApi;
import de.bermuda.hero.client.model.Hero;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

@Component
public class HeroServiceRestClient implements HeroService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HeroServiceRestClient.class);
    private final HeroApi heroApi;

    public HeroServiceRestClient(HeroApi heroApi) {
        this.heroApi = heroApi;
    }

    @Override
    public List<Hero> collectAllHeros() {
        List<Hero> heroList = new ArrayList<>();
        try {
            heroList = heroApi.collectAllHeros();
        } catch (RestClientException | ApiException e) {
            LOGGER.debug("Could not collect all Heros", e);
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
