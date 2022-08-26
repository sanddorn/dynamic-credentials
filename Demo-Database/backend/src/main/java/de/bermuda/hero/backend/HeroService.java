package de.bermuda.hero.backend;

import java.util.List;

public interface HeroService {
    List<Hero> collectAllHeros();

    void addHeroToRepository(Hero hero);
}
