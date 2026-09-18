package azhukov.chatbot.service.dunge.service;

import azhukov.chatbot.dto.ChatRequest;
import azhukov.chatbot.service.combination.CombinationService;
import azhukov.chatbot.service.dunge.ArtifactCatalog;
import azhukov.chatbot.service.dunge.DungeonRandom;
import azhukov.chatbot.service.dunge.ability.HeroAbilityService;
import azhukov.chatbot.service.dunge.data.BossInfo;
import azhukov.chatbot.service.dunge.data.GlobalBuffs;
import azhukov.chatbot.service.dunge.data.HeroClass;
import azhukov.chatbot.service.dunge.data.HeroInfo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DungeonHeroStatsTest {
    @Test
    void describesBossClassAdvantageWithoutTechnicalBooleanValues() {
        HeroInfo hero = new HeroInfo().setName("hero").setType(HeroClass.FAIRY);
        BossInfo boss = new BossInfo();
        boss.setStrong(HeroClass.FAIRY);

        HeroInfoService heroes = mock(HeroInfoService.class);
        when(heroes.getCurrent("hero")).thenReturn(hero);
        BossService bosses = mock(BossService.class);
        when(bosses.getCurrentBoss()).thenReturn(boss);
        DungeonStateService state = mock(DungeonStateService.class);
        when(state.peekBuffs()).thenReturn(new GlobalBuffs());

        DungeonService dungeon = new DungeonService(heroes, mock(HeroHealthService.class), mock(DungeonResetService.class),
                bosses, mock(BossCombatService.class), mock(DungeonEconomyService.class), state,
                mock(DungeonJournalService.class), mock(ArtifactCatalog.class), mock(DungeonRandom.class), List.of(),
                mock(CombinationService.class), mock(HeroAbilityService.class));

        String strong = dungeon.getHeroStats(new ChatRequest("hero", "!стата", true, true));
        assertTrue(strong.contains("босс усилен против вашего класса"));

        boss.setStrong(HeroClass.NECRO);
        String notStrong = dungeon.getHeroStats(new ChatRequest("hero", "!стата", true, true));
        assertFalse(notStrong.contains("босс усилен"));
    }
}
