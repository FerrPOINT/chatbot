package azhukov.chatbot.service.dunge;

import azhukov.chatbot.db.DbService;
import azhukov.chatbot.service.dunge.data.*;
import azhukov.chatbot.service.dunge.service.DungeonJournalService;
import azhukov.chatbot.service.dunge.service.HeroInfoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DungeonJournalRecoveryMapDbTest {
    @TempDir Path dbFolder;

    @Test
    void preparedAndPartialHeroMutationSurviveRestartAndReplayExactlyOnce() {
        ArtifactCatalog catalog = new ArtifactCatalog(new ObjectMapper());
        catalog.init();

        DbService firstDb = new DbService(dbFolder.toString());
        firstDb.init();
        DungeonJournalService firstJournal = new DungeonJournalService(new DungeonOperationStore(firstDb));
        HeroInfoService firstHeroes = heroes(firstDb, catalog, firstJournal);
        firstHeroes.createNew("Hero").setCoins(0);
        firstHeroes.update("Hero", hero -> hero.setCoins(0));
        DungeonOperation operation = firstJournal.prepare("recovery-1", DungeonOperation.Type.HEAL, "hero", "1:43")
                .setAmount(10);
        firstJournal.save(operation);
        firstDb.shutdown();

        DbService secondDb = new DbService(dbFolder.toString());
        secondDb.init();
        DungeonJournalService secondJournal = new DungeonJournalService(new DungeonOperationStore(secondDb));
        HeroInfoService secondHeroes = heroes(secondDb, catalog, secondJournal);
        DungeonOperation pending = secondJournal.pending().get(0);
        secondHeroes.update("hero", hero -> {
            if (hero.safeAppliedOperationIds().add(pending.getId())) hero.setCoins(hero.getCoins() + 7);
        });
        secondDb.shutdown();

        DbService thirdDb = new DbService(dbFolder.toString());
        thirdDb.init();
        DungeonJournalService thirdJournal = new DungeonJournalService(new DungeonOperationStore(thirdDb));
        HeroInfoService thirdHeroes = heroes(thirdDb, catalog, thirdJournal);
        DungeonOperation replay = thirdJournal.pending().get(0);
        thirdHeroes.update("hero", hero -> {
            if (hero.safeAppliedOperationIds().add(replay.getId())) hero.setCoins(hero.getCoins() + 7);
        });
        thirdJournal.complete(replay);
        assertEquals(7, thirdHeroes.getCurrent("hero").getCoins());
        assertEquals(0, thirdJournal.pending().size());
        thirdDb.shutdown();

        DbService fourthDb = new DbService(dbFolder.toString());
        fourthDb.init();
        DungeonJournalService fourthJournal = new DungeonJournalService(new DungeonOperationStore(fourthDb));
        HeroInfoService fourthHeroes = heroes(fourthDb, catalog, fourthJournal);
        assertEquals(7, fourthHeroes.getCurrent("hero").getCoins());
        assertEquals(0, fourthJournal.pending().size());
        fourthDb.shutdown();
    }

    private HeroInfoService heroes(DbService db, ArtifactCatalog catalog, DungeonJournalService journal) {
        return new HeroInfoService(new HeroInfoStore(db), catalog, (origin, bound) -> origin, journal);
    }
}
