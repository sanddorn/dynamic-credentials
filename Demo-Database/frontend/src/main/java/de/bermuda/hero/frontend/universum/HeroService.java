package de.bermuda.hero.frontend.universum;

import java.util.List;
import java.util.Set;

import de.bermuda.hero.client.model.Hero;

public interface HeroService {
    List<Hero> collectAllHeros();

    void addHeroToRepository(Hero hero);

}
