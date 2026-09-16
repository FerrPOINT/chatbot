package azhukov.chatbot.service.dunge;

import azhukov.chatbot.service.dunge.data.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ArtifactCatalogTest {
    private ArtifactCatalog catalog;

    @BeforeEach
    void setUp() {
        catalog = new ArtifactCatalog(new ObjectMapper());
        catalog.init();
    }

    @Test
    void migratesLegacyDefinitionsToRankOneOwnership() {
        HeroInfo hero = new HeroInfo().setArtifacts(new ArrayList<>(List.of(catalog.get("pie"))));
        assertTrue(catalog.migrateLegacy(hero).isEmpty());
        assertNull(hero.getArtifacts());
        assertEquals(1, hero.findArtifact("pie").getRank());
    }

    @Test
    void dailyGuardsDoNotStackAndLateShieldIsAdded() {
        HeroInfo hero = new HeroInfo();
        hero.addOwnedArtifact(new OwnedArtifact("pie", 1));
        hero.addOwnedArtifact(new OwnedArtifact("horse", 1));
        hero.addOwnedArtifact(new OwnedArtifact("kotovski", 3));
        assertEquals(10, catalog.dailyShield(hero)); // strongest 7 + late reward rank 3
    }

    @Test
    void stolenBossRewardRestoresAndRanksThenMaxRankCompensates() {
        HeroInfo hero = new HeroInfo().setCoins(0);
        hero.safeStolenArtifacts().add(new StolenArtifact("utan", 2, "test", LocalDateTime.now()));
        assertEquals(ArtifactCatalog.RewardResult.RESTORED_AND_RANKED, catalog.grantBossReward(hero, "utan"));
        assertEquals(3, hero.findArtifact("utan").getRank());
        hero.findArtifact("utan").setRank(5);
        assertEquals(ArtifactCatalog.RewardResult.COMPENSATED, catalog.grantBossReward(hero, "utan"));
        assertEquals(25, hero.getCoins());

        HeroInfo stolenAtMax = new HeroInfo();
        stolenAtMax.safeStolenArtifacts().add(new StolenArtifact("utan", 5, "test", LocalDateTime.now()));
        catalog.grantBossReward(stolenAtMax, "utan");
        assertEquals(5, stolenAtMax.findArtifact("utan").getRank());
        assertEquals(25, stolenAtMax.getCoins());
    }

    @Test
    void eventStyleDuplicateDoesNotRank() {
        HeroInfo hero = new HeroInfo();
        hero.addOwnedArtifact(new OwnedArtifact("utan", 1));
        hero.addOwnedArtifact(new OwnedArtifact("utan", 1));
        assertEquals(1, hero.findArtifact("utan").getRank());
    }

    @Test
    void migrationReportsButPreservesUnknownIdsAndRepairsOwnershipInvariant() {
        Artifact unknown = new Artifact("future-relic", "future", "future", List.of());
        HeroInfo hero = new HeroInfo().setArtifacts(new ArrayList<>(List.of(unknown)));
        hero.safeOwnedArtifacts().add(new OwnedArtifact("utan", 2));
        hero.safeStolenArtifacts().add(new StolenArtifact("utan", 4, "test", LocalDateTime.now()));

        assertEquals(java.util.Set.of("future-relic"), catalog.migrateLegacy(hero));
        assertNotNull(hero.findArtifact("future-relic"));
        assertNull(hero.findArtifact("utan"));
        assertEquals(4, hero.safeStolenArtifacts().get(0).getRank());
    }
}
