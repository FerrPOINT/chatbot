package azhukov.chatbot.service.dunge;

import azhukov.chatbot.db.DbService;
import azhukov.chatbot.service.dunge.data.*;
import azhukov.chatbot.service.dunge.service.DungeonCombatMath;
import azhukov.chatbot.service.dunge.service.DungeonNumbers;
import azhukov.chatbot.service.dunge.service.HeroInfoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class DungeonBalanceSimulationTest {
    @TempDir Path fixtureDir;

    @Test
    void currentAnonymizedHeroDistributionKeepsCombatPrimaryAndEconomyInTargetBand() throws Exception {
        String sourceProperty = System.getProperty("dogen.production.snapshot");
        Assumptions.assumeTrue(sourceProperty != null && !sourceProperty.isBlank());
        Files.copy(Path.of(sourceProperty).resolve("DUNGE.db"), fixtureDir.resolve("DUNGE.db"));
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        ArtifactCatalog catalog = new ArtifactCatalog(mapper); catalog.init();
        DbService db = new DbService(fixtureDir.toString()); db.init();
        HeroInfoService heroService = new HeroInfoService(new HeroInfoStore(db), catalog, (o, b) -> o,
                new azhukov.chatbot.service.dunge.service.DungeonJournalService(new DungeonOperationStore(db)));
        heroService.migrateAll();
        List<HeroInfo> fixture = heroService.all();
        db.shutdown();

        BossInfo boss = new BossInfo(); boss.setStage(43); boss.setLevel(250); boss.setMaxHp(250_000); boss.setStrong(HeroClass.DEFENDER); boss.setWeak(HeroClass.SAMURAI);
        Random random = new Random(0xD06E2026L);
        long normalDamage = 0, siegeDamage = 0;
        int deaths = 0, heals = 0, attacks = 0, activeSessions = 0;
        for (int day = 0; day < 30; day++) {
            for (HeroInfo hero : fixture) {
                if (random.nextDouble() >= 0.18) continue;
                activeSessions++;
                hero.setDamageGot(HeroDamage.NONE); hero.setShield(catalog.dailyShield(hero));
                hero.setCoinsEarnedToday(0); hero.setHealUsedToday(false);
                earn(hero, 1, catalog);
                for (int fight = 0; fight < 2 && !hero.isDead(); fight++) {
                    if (hero.getDamageGot().getValue() >= HeroDamage.BIG.getValue() && hero.getCoins() >= Math.max(5, 10 - catalog.healDiscount(hero)) && !hero.isHealUsedToday()) {
                        hero.setCoins(hero.getCoins() - Math.max(5, 10 - catalog.healDiscount(hero)));
                        hero.heal(1); hero.setHealUsedToday(true); heals++;
                    }
                    int incoming = 0, rounds = 0, projectedShield = hero.getShield();
                    boolean risky = hero.getDamageGot().getValue() - projectedShield > HeroDamage.MEDIUM.getValue();
                    if (risky) { incoming = roll(random, boss.getStrong() == hero.getType()); rounds = 1; }
                    else while (hero.getDamageGot().getValue() + incoming - projectedShield < HeroDamage.BIG.getValue() && rounds < 32) {
                        incoming += roll(random, boss.getStrong() == hero.getType()); rounds++;
                    }
                    long attack = catalog.attack(hero, boss);
                    long dealt = DungeonNumbers.multiply(attack, Math.max(1, rounds));
                    normalDamage = DungeonNumbers.add(normalDamage, dealt); attacks++;
                    earn(hero, 1 + Math.min(4, DungeonCombatMath.ceilDiv(1000L * dealt, boss.getMaxHp())), catalog);
                    int absorbed = Math.min(hero.getShield(), incoming); hero.setShield(hero.getShield() - absorbed);
                    hero.setDamageGot(HeroDamage.getByValue(hero.getDamageGot().getValue() + incoming - absorbed));
                    if (hero.getDamageGot() == HeroDamage.DEAD) { deaths++; hero.setCoins(0); }
                }
                // A siege-oriented cohort donates each accumulated ten-coin bundle.
                if (!hero.isDead() && hero.getCoins() >= 10) {
                    hero.setCoins(hero.getCoins() - 10);
                    siegeDamage += boss.getMaxHp() * 10L * 450L / 1_000_000L;
                }
            }
        }
        double economyShare = (double) siegeDamage / (double) (normalDamage + siegeDamage);
        System.out.printf("anonymizedHeroes=%d activeSessions=%d attacks=%d deaths=%d heals=%d normalDamage=%d siegeDamage=%d economyShare=%.4f%n",
                fixture.size(), activeSessions, attacks, deaths, heals, normalDamage, siegeDamage, economyShare);
        assertFalse(fixture.isEmpty());
        assertTrue(normalDamage > siegeDamage, "ordinary combat must remain primary");
        assertTrue(economyShare >= 0.10 && economyShare <= 0.25, "economy share=" + economyShare);
        assertTrue(deaths > 0, "death must remain a meaningful risk");
        assertTrue(heals > 0, "healing must be used but remains once per day");
        assertTrue(normalDamage > 0 && attacks > 0, "any activity must produce permanent progress");
    }

    private static int roll(Random random, boolean strong) { return random.nextInt(HeroDamage.HUGE.getValue() - (strong ? 2 : 1)) + (strong ? 2 : 1); }

    private static void earn(HeroInfo hero, long amount, ArtifactCatalog catalog) {
        long cap = 15L + catalog.dailyCoinCapBonus(hero);
        long credited = Math.min(amount, Math.max(0, cap - hero.getCoinsEarnedToday()));
        hero.setCoins(DungeonNumbers.add(hero.getCoins(), credited));
        hero.setCoinsEarnedToday(DungeonNumbers.add(hero.getCoinsEarnedToday(), credited));
    }
}
