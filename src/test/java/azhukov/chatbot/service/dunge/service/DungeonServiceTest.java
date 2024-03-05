package azhukov.chatbot.service.dunge.service;

import azhukov.chatbot.service.combination.CombinationService;
import azhukov.chatbot.service.dunge.ArticfactService;
import azhukov.chatbot.service.dunge.data.*;
import azhukov.chatbot.service.dunge.event.DungeEvent;
import azhukov.chatbot.service.store.DailyStore;
import azhukov.chatbot.util.RangesContainer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class DungeonServiceTest {
    @Mock
    private HeroInfoService heroInfoService;
    @Mock
    private BossService bossService;
    @Mock
    private ArticfactService articfactService;
    @Mock
    private DailyStore dailyStore;
    @Mock
    private List<DungeEvent> events;
    @Mock
    private CombinationService combinationService;
    @Mock
    private RangesContainer<DungeEvent> eventsContainer;
    @InjectMocks
    private DungeonService dungeonService;

    @Test
    void testEarnXp() {
        HeroInfo hero = new HeroInfo();
        int initialExp = 3000;
        hero.setExperience(initialExp);
        BossInfo boss = new BossInfo();
        boss.setStage(2);
        FightResult fight = new FightResult();
        fight.setHero(hero);
        fight.setBoss(boss);
        fight.setFightsNumber(2);
        fight.setDamageDone(IntStream.range(0, fight.getFightsNumber()).map(operand -> hero.getAttack(boss)).sum());
        fight.setDamageReceived(HeroDamage.MEDIUM);

        for (int i = 0; i < 1000; i++) {
            dungeonService.earnXP(fight);
            int exp = fight.getExp();
            System.out.println("exp = " + exp);
        }

        hero.setArtifacts(new ArrayList<>(List.of(new Artifact(null, null, null, null), new Artifact(null, null, null, null))));

        String s = DungeonService.tryToSteal(100, hero);

        assertEquals(1, hero.getArtifacts().size());
    }
}