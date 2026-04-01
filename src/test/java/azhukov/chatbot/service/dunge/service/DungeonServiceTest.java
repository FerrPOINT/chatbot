package azhukov.chatbot.service.dunge.service;

import azhukov.chatbot.service.combination.CombinationService;
import azhukov.chatbot.service.dunge.ArticfactService;
import azhukov.chatbot.service.dunge.ability.AbilitiesData;
import azhukov.chatbot.service.dunge.data.*;
import azhukov.chatbot.service.dunge.event.DungeEvent;
import azhukov.chatbot.service.store.DailyStore;
import azhukov.chatbot.util.RangesContainer;
import azhukov.chatbot.dto.ChatRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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
        fight.setNextHeroBuffs(new AbilitiesData());

        for (int i = 0; i < 1000; i++) {
            dungeonService.earnXP(fight);
            int exp = fight.getExp();
            System.out.println("exp = " + exp);
        }

        hero.setArtifacts(new ArrayList<>(List.of(new Artifact(null, null, null, null), new Artifact(null, null, null, null))));

        String s = DungeonService.tryToSteal(100, hero);

        assertEquals(1, hero.getArtifacts().size());
    }

    @Test
    void testEarnXpAddsCoins() {
        HeroInfo hero = new HeroInfo();
        hero.setExperience(1000);
        hero.setCoins(0);
        BossInfo boss = new BossInfo();
        boss.setStage(5);
        boss.setLevel(5);
        boss.setDamageReceived(0);
        FightResult fight = new FightResult();
        fight.setHero(hero);
        fight.setBoss(boss);
        fight.setFightsNumber(3);
        fight.setDamageDone(300);
        fight.setDamageReceived(HeroDamage.MEDIUM);
        fight.setNextHeroBuffs(new AbilitiesData());

        dungeonService.earnXP(fight);

        assertEquals(7, fight.getMoneyPrize());
        assertEquals(7, hero.getCoins());
    }

    @Test
    void testBossRushDealsDamageAndConsumesCoins() {
        String user = "ferrpoint";
        HeroInfo hero = new HeroInfo()
                .setName(user)
                .setCoins(80)
                .setDamageGot(HeroDamage.NONE);
        BossInfo boss = new BossInfo();
        boss.setStage(43);
        boss.setLevel(250);
        boss.setName("Гигантская Уточка Утан");

        when(heroInfoService.getCurrent(user)).thenReturn(hero);
        when(bossService.getCurrentBoss()).thenReturn(boss);
        when(bossService.damage(eq(user), anyInt())).thenAnswer(invocation -> {
            int damage = invocation.getArgument(1);
            boss.setDamageReceived(boss.getDamageReceived() + damage);
            return boss;
        });
        doAnswer(invocation -> {
            String name = invocation.getArgument(0);
            java.util.function.Consumer<HeroInfo> updater = invocation.getArgument(1);
            if (user.equals(name)) {
                updater.accept(hero);
            }
            return null;
        }).when(heroInfoService).update(eq(user), any());

        String response = dungeonService.useBossRush(new ChatRequest(user, "!рывок", false, false));

        assertEquals(40, hero.getCoins());
        assertEquals(30000, boss.getDamageReceived());
        assertTrue(response.contains("30000"));
        assertTrue(response.contains("220000"));
    }
}