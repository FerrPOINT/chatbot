package azhukov.chatbot.service.dunge.event;

import azhukov.chatbot.service.dunge.ArtifactCatalog;
import azhukov.chatbot.service.dunge.DungeonRandom;
import azhukov.chatbot.service.dunge.data.HeroInfo;
import azhukov.chatbot.service.dunge.service.BossService;
import azhukov.chatbot.service.weight.Weight;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ArtifactEventTest {
    @Test
    void localWeightAndDuplicateExperienceChangeArePreserved() {
        BossService bosses = mock(BossService.class);
        when(bosses.getOldRewards()).thenReturn(Set.of());
        DungeonRandom random = (o, b) -> o;
        ArtifactEvent event = new ArtifactEvent(bosses, mock(ArtifactCatalog.class), random);
        HeroInfo hero = new HeroInfo().setExperience(50);
        event.handle(hero);
        assertEquals(1_050, hero.getExperience());
        assertEquals(Weight.HIGHEST, event.getWeight());
    }
}
