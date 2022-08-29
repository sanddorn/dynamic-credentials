package de.bermuda.hero.frontend.universum;

import java.util.Objects;

import de.bermuda.hero.client.model.Hero;

public class NewHeroModel {

    private Hero hero;

    public Hero getHero() {
        return hero;
    }

    public void setHero(Hero hero) {
        this.hero = hero;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        NewHeroModel that = (NewHeroModel) o;
        return Objects.equals(hero, that.hero);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hero);
    }

    @Override
    public String toString() {
        return "NewHeroModel{" +
                "hero=" + hero +
                '}';
    }
}
