package de.bermuda.hero.backend;

import java.util.List;
import java.util.stream.Collectors;

import de.bermuda.hero.api.HeroApi;
import de.bermuda.hero.model.Hero;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HeroRestController implements HeroApi {
    private final HeroService heroService;

    public HeroRestController(HeroService heroService) {
        this.heroService = heroService;
    }

    @Override
    public ResponseEntity<Void> addHeroToRepository(de.bermuda.hero.model.Hero body) {
        heroService.addHeroToRepository(HeroMapper.INSTANCE.map(body));
        return ResponseEntity.created(null).build();
    }

    @Override
    public ResponseEntity<List<Hero>> collectAllHeros() {
        return ResponseEntity.ok(
                heroService.collectAllHeros()
                           .stream()
                           .map(HeroMapper.INSTANCE::reverseMap)
                           .collect(Collectors.toList()));
    }
}
