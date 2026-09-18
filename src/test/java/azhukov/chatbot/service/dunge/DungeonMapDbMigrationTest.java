package azhukov.chatbot.service.dunge;

import azhukov.chatbot.db.DbService;
import azhukov.chatbot.db.DbType;
import azhukov.chatbot.service.dunge.data.*;
import azhukov.chatbot.service.dunge.service.BossService;
import azhukov.chatbot.service.dunge.service.HeroInfoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DungeonMapDbMigrationTest {
    @TempDir Path temp;

    @Test
    void migratesLegacyHeroAndBossWithoutChangingCurrentProgressAndSurvivesRestart() {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        DbService first = new DbService(temp.toString());
        first.init();
        HeroInfoStore oldHeroes = new HeroInfoStore(first);
        BossStore oldBosses = new BossStore(first);
        HeroInfo oldHero = new HeroInfo().setName("Tester").setExperience(12_345).setDamageGot(HeroDamage.MEDIUM)
                .setType(HeroClass.SAMURAI).setArtifacts(List.of(new Artifact("utan", "legacy", "legacy", List.of())));
        oldHeroes.put("tester", oldHero);
        BossInfo utan = new BossInfo();
        utan.setStage(43); utan.setLevel(250); utan.setName("Утан"); utan.setDamageReceived(128_240);
        utan.setCycle(0); utan.setMaxHp(0); utan.setDamagedHeroes(new HashSet<>(List.of("tester")));
        oldBosses.put("CURRENT_BOSS", utan);
        BossInfo archived = new BossInfo(); archived.setStage(42); archived.setLevel(200); archived.setDamageReceived(200_000);
        oldBosses.put("BOSS_42", archived);
        first.getDb(DbType.DUNGE).commit(); first.getDb(DbType.DUNGE_BOSS).commit();
        first.shutdown();

        DbService second = new DbService(temp.toString());
        second.init();
        ArtifactCatalog catalog = new ArtifactCatalog(mapper); catalog.init();
        HeroInfoService heroService = new HeroInfoService(new HeroInfoStore(second), catalog, (o, b) -> o,
                new azhukov.chatbot.service.dunge.service.DungeonJournalService(new DungeonOperationStore(second)));
        heroService.migrateAll();
        BossService bossService = new BossService(mapper, new BossStore(second), catalog, new DungeonTime());
        bossService.init();

        HeroInfo migrated = heroService.getCurrent("TESTER");
        assertEquals(12_345, migrated.getExperience());
        assertEquals(1, migrated.findArtifact("utan").getRank());
        assertNull(migrated.getArtifacts());
        BossInfo current = bossService.getCurrentBoss();
        assertEquals("1:43", current.getInstanceId());
        assertEquals(250_000, current.getMaxHp());
        assertEquals(128_240, current.getDamageReceived());
        assertEquals(121_760, current.getCurrentHp());
        assertEquals(Set.of("tester"), current.getDamagedHeroes());
        assertNotNull(new BossStore(second).get("BOSS_1_42"));
        assertNull(new BossStore(second).get("BOSS_42"));

        for (DbType type : DbType.values()) second.getDb(type);
        second.shutdown();
        assertTrue(Files.exists(temp.resolve("DUNGE_ECONOMY.db")));
        assertEquals(9, DbType.values().length);
    }
}
