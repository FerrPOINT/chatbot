package azhukov.chatbot.service.dunge.service;

import azhukov.chatbot.service.dunge.data.BossInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DungeonStartupService {
    private final HeroInfoService heroes;
    private final DungeonResetService reset;
    private final DungeonEconomyService economy;
    private final BossCombatService combat;
    private final DungeonStateService state;
    private final BossService bosses;
    private final azhukov.chatbot.service.dunge.ability.HeroAbilityService abilities;
    private final DungeonService dungeon;

    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        HeroInfoService.MigrationReport report = heroes.migrateAll();
        if (!report.getUnknownArtifacts().isEmpty()) {
            log.error("Dungeon migration retained unknown artifact ids in report: {}", report.getUnknownArtifacts());
            state.update(s -> {
                if (s.getMigrationUnknownArtifacts() == null) s.setMigrationUnknownArtifacts(new java.util.LinkedHashMap<>());
                report.getUnknownArtifacts().forEach((hero, ids) ->
                        s.getMigrationUnknownArtifacts().put(hero, new java.util.ArrayList<>(ids)));
            });
        }
        heroes.replayPendingArtifactGrants();
        abilities.replayPending();
        economy.replayPending();
        combat.replayPending();
        dungeon.replayPendingActions();
        reset.replayPending();
        reset.catchUp();
        BossInfo boss = bosses.getCurrentBoss();
        log.info("Dungeon migration complete: heroes={}, unknownArtifactOwners={}", report.getHeroes(), report.getUnknownArtifacts().size());
        if (boss != null) {
            log.info("Dungeon active boss: instance={}, hp={}, maxHp={}, participants={}, startedAtEstimated={}",
                    boss.getInstanceId(), boss.getCurrentHp(), boss.getMaxHp(), boss.safeDamagedHeroes().size(), boss.isStartedAtEstimated());
        }
    }
}
