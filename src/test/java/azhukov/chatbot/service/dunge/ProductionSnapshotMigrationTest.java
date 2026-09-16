package azhukov.chatbot.service.dunge;

import azhukov.chatbot.db.DbService;
import azhukov.chatbot.service.dunge.data.*;
import azhukov.chatbot.service.dunge.service.BossService;
import azhukov.chatbot.service.dunge.service.DungeonStateService;
import azhukov.chatbot.service.dunge.service.HeroInfoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ProductionSnapshotMigrationTest {
    @TempDir Path migratedDir;

    @Test
    void productionCopyMigratesWithoutResettingActiveBossOrHeroes() throws Exception {
        String sourceProperty = System.getProperty("dogen.production.snapshot");
        Assumptions.assumeTrue(sourceProperty != null && !sourceProperty.isBlank());
        Path source = Path.of(sourceProperty);
        Files.copy(source.resolve("DUNGE.db"), migratedDir.resolve("DUNGE.db"), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(source.resolve("DUNGE_BOSS.db"), migratedDir.resolve("DUNGE_BOSS.db"), StandardCopyOption.REPLACE_EXISTING);

        DbService beforeDb = new DbService(migratedDir.toString()); beforeDb.init();
        BossInfo beforeBoss = new BossStore(beforeDb).get("CURRENT_BOSS");
        Set<String> beforeParticipants = new HashSet<>(beforeBoss.getDamagedHeroes());
        int[] beforeHeroes = {0};
        new HeroInfoStore(beforeDb).handleAll(h -> beforeHeroes[0]++);
        long beforeDamage = beforeBoss.getDamageReceived();
        long beforeHp = beforeBoss.getCurrentHp();
        beforeDb.shutdown();

        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        DbService migrationDb = new DbService(migratedDir.toString()); migrationDb.init();
        ArtifactCatalog catalog = new ArtifactCatalog(mapper); catalog.init();
        HeroInfoService heroes = new HeroInfoService(new HeroInfoStore(migrationDb), catalog, (o, b) -> o, mockJournal(migrationDb));
        HeroInfoService.MigrationReport report = heroes.migrateAll();
        BossService bosses = new BossService(mapper, new BossStore(migrationDb), catalog, new DungeonTime()); bosses.init();
        new DungeonStateService(new DungeonStateStore(migrationDb)).get();
        BossInfo migrated = bosses.getCurrentBoss();

        assertEquals(beforeHeroes[0], report.getHeroes());
        assertTrue(report.getUnknownArtifacts().isEmpty(), "unknown artifact ids: " + report.getUnknownArtifacts());
        assertEquals(beforeDamage, migrated.getDamageReceived());
        assertEquals(beforeHp, migrated.getCurrentHp());
        assertEquals(beforeParticipants, migrated.getDamagedHeroes());
        assertEquals(1, migrated.getCycle());
        assertTrue(migrated.getStoredMaxHp() > 0);
        heroes.all().forEach(hero -> {
            assertNull(hero.getArtifacts());
            assertTrue(hero.getExperience() >= 0);
        });
        migrationDb.shutdown();

        DbService restartDb = new DbService(migratedDir.toString()); restartDb.init();
        BossInfo restarted = new BossStore(restartDb).get("CURRENT_BOSS");
        assertEquals(beforeDamage, restarted.getDamageReceived());
        assertEquals(beforeParticipants, restarted.getDamagedHeroes());
        assertNotNull(new DungeonStateStore(restartDb).get("GLOBAL"));
        restartDb.shutdown();
    }

    private azhukov.chatbot.service.dunge.service.DungeonJournalService mockJournal(DbService db) {
        return new azhukov.chatbot.service.dunge.service.DungeonJournalService(new DungeonOperationStore(db));
    }
}
