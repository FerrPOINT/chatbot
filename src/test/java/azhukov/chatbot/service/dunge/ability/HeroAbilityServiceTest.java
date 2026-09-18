package azhukov.chatbot.service.dunge.ability;

import azhukov.chatbot.service.dunge.ArtifactCatalog;
import azhukov.chatbot.service.dunge.DungeonRandom;
import azhukov.chatbot.service.dunge.data.*;
import azhukov.chatbot.service.dunge.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class HeroAbilityServiceTest {
    private final Map<String, HeroInfo> data = new HashMap<>();
    private HeroInfoService heroes;
    private DungeonState dungeonState;
    private HeroAbilityService service;
    private BossService bosses;
    private BossCombatService combat;
    private ArtifactCatalog artifacts;

    @BeforeEach
    void setUp() {
        heroes = mock(HeroInfoService.class);
        when(heroes.getCurrent(anyString())).thenAnswer(i -> data.get(i.getArgument(0)));
        doAnswer(invocation -> {
            String name = invocation.getArgument(0);
            Consumer<HeroInfo> update = invocation.getArgument(1);
            update.accept(data.get(name));
            return null;
        }).when(heroes).update(anyString(), any());
        DungeonStateService state = mock(DungeonStateService.class);
        dungeonState = new DungeonState();
        doAnswer(invocation -> {
            Consumer<DungeonState> update = invocation.getArgument(0);
            update.accept(dungeonState);
            return null;
        }).when(state).update(any());
        DungeonRandom random = (origin, bound) -> origin;
        DungeonJournalService journal = mock(DungeonJournalService.class);
        when(journal.prepare(any(), anyString(), nullable(String.class))).thenAnswer(i -> new DungeonOperation()
                .setId("ability:" + i.getArgument(1)).setType(i.getArgument(0)).setHero(i.getArgument(1)).setBossInstance(i.getArgument(2)));
        HeroHealthService health = mock(HeroHealthService.class);
        when(health.heal(any(HeroInfo.class), anyInt())).thenAnswer(i -> { ((HeroInfo) i.getArgument(0)).heal(i.getArgument(1)); return true; });
        when(health.reviveEarly(any(HeroInfo.class))).thenAnswer(i -> {
            ((HeroInfo) i.getArgument(0)).setDeadTime(null).setDamageGot(HeroDamage.MEDIUM);
            return true;
        });
        bosses = mock(BossService.class);
        combat = mock(BossCombatService.class);
        artifacts = mock(ArtifactCatalog.class);
        service = new HeroAbilityService(heroes, health, state, bosses, combat, artifacts, random, journal);
    }

    @Test
    void incompatibleFairyTargetDoesNotConsumeAbility() {
        HeroInfo fairy = hero("fairy", HeroClass.FAIRY, HeroDamage.NONE, 1_000);
        HeroInfo healthy = hero("healthy", HeroClass.SAILOR, HeroDamage.NONE, 1_000);
        String message = service.useHeroAbility(fairy, healthy);
        assertTrue(message.contains("нет подходящего"));
        assertFalse(fairy.isSpecialAbilityUsed());
    }

    @Test
    void fairyReallyHealsTwoLevels() {
        HeroInfo fairy = hero("fairy", HeroClass.FAIRY, HeroDamage.NONE, 1_000);
        HeroInfo wounded = hero("wounded", HeroClass.SAILOR, HeroDamage.BIG, 1_000);
        service.useHeroAbility(fairy, wounded);
        assertEquals(HeroDamage.SLIGHT, wounded.getDamageGot());
        assertTrue(fairy.isSpecialAbilityUsed());
    }

    @Test
    void globalBuffsArePersistedAndCappedByStateModel() {
        HeroInfo sailor = hero("sailor", HeroClass.SAILOR, HeroDamage.NONE, 1_000);
        service.useHeroAbility(sailor, null);
        assertEquals(20, dungeonState.buffs().attackPercent());
    }

    @Test
    void rogueCopiesWithoutRemovingVictimArtifact() {
        HeroInfo rogue = hero("rogue", HeroClass.ROGUE, HeroDamage.NONE, 2_000);
        HeroInfo victim = hero("victim", HeroClass.SAILOR, HeroDamage.NONE, 1_000);
        victim.addOwnedArtifact(new OwnedArtifact("pie", 4));
        service.useHeroAbility(rogue, victim);
        assertEquals(4, victim.findArtifact("pie").getRank());
        assertEquals(1, rogue.findArtifact("pie").getRank());
    }

    @Test
    void defenderShamanNobleAndPrisonerUseTheirDocumentedFields() {
        HeroInfo defender = hero("defender", HeroClass.DEFENDER, HeroDamage.NONE, 1_000);
        service.useHeroAbility(defender, null);
        assertEquals(2, defender.getShield());

        HeroInfo shaman = hero("shaman", HeroClass.SHAMAN, HeroDamage.NONE, 1_000);
        service.useHeroAbility(shaman, null);
        assertEquals(2, dungeonState.buffs().shield());

        HeroInfo noble = hero("noble", HeroClass.NOBLE, HeroDamage.NONE, 1_000);
        service.useHeroAbility(noble, null);
        assertEquals(20, dungeonState.buffs().expPercent());
        assertEquals(0, dungeonState.buffs().attackPercent());

        HeroInfo prisoner = hero("prisoner", HeroClass.PRISONER, HeroDamage.NONE, 1_000);
        service.useHeroAbility(prisoner, null);
        assertEquals(50, prisoner.getRebornPercentage());
    }

    @Test
    void necromancerRequiresLowerLevelAndDoesNotReapplyDeathPenalty() {
        HeroInfo necro = hero("necro", HeroClass.NECRO, HeroDamage.NONE, 5_000);
        HeroInfo corpse = hero("corpse", HeroClass.SAILOR, HeroDamage.DEAD, 2_000)
                .setDeadTime(java.time.LocalDateTime.of(2026, 9, 16, 12, 0));
        service.useHeroAbility(necro, corpse);
        assertEquals(HeroDamage.MEDIUM, corpse.getDamageGot());
        assertEquals(2_000, corpse.getExperience());

        HeroInfo weakNecro = hero("weak", HeroClass.NECRO, HeroDamage.NONE, 1_000);
        HeroInfo veteran = hero("veteran", HeroClass.SAILOR, HeroDamage.DEAD, 5_000)
                .setDeadTime(java.time.LocalDateTime.of(2026, 9, 16, 12, 0));
        assertTrue(service.useHeroAbility(weakNecro, veteran).contains("ниже уровнем"));
        assertFalse(weakNecro.isSpecialAbilityUsed());
    }

    @Test
    void rogueTreatsStolenCopyAsDuplicateExperience() {
        HeroInfo rogue = hero("rogue", HeroClass.ROGUE, HeroDamage.NONE, 2_000);
        rogue.safeStolenArtifacts().add(new StolenArtifact("pie", 3, "test", java.time.LocalDateTime.now()));
        HeroInfo victim = hero("victim", HeroClass.SAILOR, HeroDamage.NONE, 1_000);
        victim.addOwnedArtifact(new OwnedArtifact("pie", 4));

        service.useHeroAbility(rogue, victim);

        assertEquals(3_000, rogue.getExperience());
        assertNull(rogue.findArtifact("pie"));
        assertEquals(4, victim.findArtifact("pie").getRank());
    }

    @Test
    void samuraiUsesTheCommonBossDamageFlow() {
        BossInfo boss = new BossInfo(); boss.setCycle(1); boss.setStage(43); boss.setMaxHp(1_000);
        when(bosses.getCurrentBoss()).thenReturn(boss);
        HeroInfo samurai = hero("samurai", HeroClass.SAMURAI, HeroDamage.NONE, 2_000);
        when(artifacts.attack(samurai, boss)).thenReturn(123L);
        when(combat.damageWithId(anyString(), eq("samurai"), eq(123L), eq(DungeonOperation.Type.ABILITY_DAMAGE)))
                .thenReturn(new BossCombatService.DamageResult(123, false, boss, null));

        assertTrue(service.useHeroAbility(samurai, null).contains("123"));
        verify(combat).damageWithId(anyString(), eq("samurai"), eq(123L), eq(DungeonOperation.Type.ABILITY_DAMAGE));
        assertTrue(samurai.isSpecialAbilityUsed());
    }

    @Test
    void werewolfUsesInjectedRandomSourceAndPersistsResolvedEffect() {
        HeroInfo werewolf = hero("werewolf", HeroClass.WEREWOLF, HeroDamage.NONE, 2_000);
        String result = service.useHeroAbility(werewolf, null);
        assertTrue(result.contains(HeroClass.SAILOR.getLabel()));
        assertEquals(20, dungeonState.buffs().attackPercent());
        assertTrue(werewolf.isSpecialAbilityUsed());
    }

    private HeroInfo hero(String name, HeroClass type, HeroDamage damage, long xp) {
        HeroInfo hero = new HeroInfo().setName(name).setType(type).setDamageGot(damage).setExperience(xp);
        data.put(name, hero);
        return hero;
    }
}
