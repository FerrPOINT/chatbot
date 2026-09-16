package azhukov.chatbot.service.dunge.service;

import azhukov.chatbot.service.dunge.ArtifactCatalog;
import azhukov.chatbot.service.dunge.data.BossInfo;
import azhukov.chatbot.service.dunge.data.DungeonOperation;
import azhukov.chatbot.service.dunge.data.HeroInfo;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BossCombatService {
    private final BossService bosses;
    private final HeroInfoService heroes;
    private final ArtifactCatalog artifacts;
    private final DungeonJournalService journal;

    public DamageResult damage(String heroName, long requested, DungeonOperation.Type type) {
        return damageWithId(null, heroName, requested, type);
    }

    public DamageResult damageWithId(String operationId, String heroName, long requested, DungeonOperation.Type type) {
        synchronized (bosses) {
            BossInfo boss = bosses.getCurrentBoss();
            if (boss == null) return new DamageResult(0, false, null, null);
            DungeonOperation operation = operationId == null
                    ? journal.prepare(type, heroName, boss.getInstanceId())
                    : journal.prepare(operationId, type, heroName, boss.getInstanceId());
            if (operation.getStatus() == DungeonOperation.Status.COMPLETED) {
                return new DamageResult(0, false, boss, null);
            }
            long real = Math.min(Math.max(0L, requested), boss.getCurrentHp());
            operation.setAmount(real);
            journal.save(operation);
            DamageResult result = applyPreparedLocked(operation);
            journal.complete(operation);
            return result;
        }
    }

    public DamageResult applyPrepared(DungeonOperation operation) {
        synchronized (bosses) {
            return applyPreparedLocked(operation);
        }
    }

    private DamageResult applyPreparedLocked(DungeonOperation operation) {
        BossInfo boss = bosses.getCurrentBoss();
        if (boss == null || !boss.getInstanceId().equals(operation.getBossInstance())) {
            return new DamageResult(0, false, boss, null);
        }
        final long[] real = {0L};
        final long[] contribution = {0L};
        long hpBefore = boss.getCurrentHp();
        bosses.updateCurrentBoss(current -> {
            if (current.safeAppliedOperationIds().add(operation.getId())) {
                real[0] = current.dealDamage(operation.getAmount());
                contribution[0] = real[0];
                if (real[0] > 0) {
                    current.safeDamagedHeroes().add(operation.getHero());
                    current.safeDamageByHero().merge(operation.getHero(), real[0], DungeonNumbers::add);
                    current.addDamageBySource(operation.getType(), real[0]);
                }
            } else {
                contribution[0] = operation.getAmount();
            }
        });
        if (contribution[0] > 0) {
            heroes.update(operation.getHero(), hero -> {
                if (hero.safeAppliedOperationIds().add(operation.getId())) {
                    hero.setBossDamage(DungeonNumbers.add(hero.getBossDamage(), contribution[0]));
                }
            });
        }
        BossInfo defeated = bosses.getCurrentBoss();
        if (defeated != null) DungeonMetrics.damage(operation.getType().name(), operation.getBossInstance(), hpBefore,
                defeated.getCurrentHp(), real[0], operation.getHero());
        boolean killed = defeated != null && defeated.isDead();
        BossInfo next = null;
        if (killed) {
            grantRewardsExactlyOnce(defeated, operation.getHero());
            next = bosses.transitionAfterVictory();
        }
        return new DamageResult(real[0], killed, defeated, next);
    }

    private void grantRewardsExactlyOnce(BossInfo defeated, String killer) {
        if (defeated.isRewardsGranted()) return;
        DungeonOperation rewardOp = journal.prepare("reward:" + defeated.getInstanceId(), DungeonOperation.Type.REWARD, killer, defeated.getInstanceId());
        journal.save(rewardOp);
        List<String> participants = new ArrayList<>(defeated.safeDamagedHeroes());
        for (String participant : participants) {
            if (heroes.getCurrent(participant) == null) continue;
            heroes.update(participant, hero -> {
                for (String reward : defeated.safeRewards()) {
                    String applied = rewardOp.getId() + ":" + reward;
                    if (hero.safeAppliedOperationIds().add(applied)) artifacts.grantBossReward(hero, reward);
                }
                if (participant.equals(killer) && hero.safeAppliedOperationIds().add(rewardOp.getId() + ":killer")) {
                    hero.setCoins(DungeonNumbers.add(hero.getCoins(), 10L));
                }
            });
        }
        bosses.updateCurrentBoss(current -> {
            current.setRewardsGranted(true);
            current.safeAppliedOperationIds().add(rewardOp.getId());
        });
        journal.complete(rewardOp);
    }

    /** Replays cross-store operations left PREPARED by a process interruption. */
    public void replayPending() {
        for (DungeonOperation operation : journal.pending()) {
            if (operation.getType() == DungeonOperation.Type.ABILITY_DAMAGE) {
                journal.replay(operation);
                applyPrepared(operation);
                journal.complete(operation);
            } else if (operation.getType() == DungeonOperation.Type.REWARD) {
                journal.replay(operation);
                BossInfo boss = bosses.getCurrentBoss();
                if (boss != null && boss.getInstanceId().equals(operation.getBossInstance())) {
                    grantRewardsExactlyOnce(boss, operation.getHero());
                }
                journal.complete(operation);
            }
        }
    }

    @Data
    public static class DamageResult {
        private final long realDamage;
        private final boolean killed;
        private final BossInfo defeatedBoss;
        private final BossInfo nextBoss;
    }
}
