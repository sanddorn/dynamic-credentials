package de.bermuda.hero.backend;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class HeroServiceImpl implements HeroService {
    private final static Logger LOGGER = LoggerFactory.getLogger(HeroServiceImpl.class);

    private final HeroRepository repository;

    HeroServiceImpl(HeroRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Hero> collectAllHeros() {
        return repository.findAll();
    }

    @Override
    public void addHeroToRepository(Hero hero) {
        LOGGER.info("addHeroToRepository: ", hero);
        Hero response = repository.save(hero);
        LOGGER.info("added Hero: {}", response);
    }
}
