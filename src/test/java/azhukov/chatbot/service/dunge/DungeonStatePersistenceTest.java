package azhukov.chatbot.service.dunge;

import azhukov.chatbot.db.DbService;
import azhukov.chatbot.service.dunge.data.DungeonStateStore;
import azhukov.chatbot.service.dunge.data.HeroInfo;
import azhukov.chatbot.service.dunge.service.DungeonStateService;
import azhukov.chatbot.service.dunge.service.HeroInfoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DungeonStatePersistenceTest {
    @TempDir Path dbFolder;

    @Test
    void globalBuffAndUnboundedPreviousHeroHistorySurviveRestart() {
        DbService firstDb = new DbService(dbFolder.toString()); firstDb.init();
        DungeonStateService first = new DungeonStateService(new DungeonStateStore(firstDb));
        first.update(state -> {
            state.touchHero("old-existing");
            for (int i = 0; i < 150; i++) state.touchHero("missing-" + i);
            state.touchHero("current");
            state.buffs().addAttack();
        });
        firstDb.shutdown();

        DbService secondDb = new DbService(dbFolder.toString()); secondDb.init();
        DungeonStateService restarted = new DungeonStateService(new DungeonStateStore(secondDb));
        HeroInfoService heroes = mock(HeroInfoService.class);
        when(heroes.getCurrent("old-existing")).thenReturn(new HeroInfo().setName("old-existing"));

        assertEquals(20, restarted.peekBuffs().attackPercent());
        assertEquals("old-existing", restarted.previousExisting("current", heroes).getName());
        secondDb.shutdown();
    }
}
