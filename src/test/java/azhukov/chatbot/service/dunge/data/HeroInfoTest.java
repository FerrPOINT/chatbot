package azhukov.chatbot.service.dunge.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HeroInfoTest {
    @Test
    void experienceNeverBecomesNegativeOrOverflows() {
        HeroInfo hero = new HeroInfo().setExperience(10);
        hero.addExp(-20);
        assertEquals(0, hero.getExperience());
        hero.setExperience(Long.MAX_VALUE - 1).addExp(100);
        assertEquals(Long.MAX_VALUE, hero.getExperience());
    }

    @Test
    void artifactCannotBeOwnedTwice() {
        HeroInfo hero = new HeroInfo();
        hero.addOwnedArtifact(new OwnedArtifact("a", 1));
        hero.addOwnedArtifact(new OwnedArtifact("a", 4));
        assertEquals(1, hero.getOwnedArtifacts().size());
        assertEquals(1, hero.findArtifact("a").getRank());
    }
}
