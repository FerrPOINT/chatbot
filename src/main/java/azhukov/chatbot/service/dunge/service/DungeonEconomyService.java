package azhukov.chatbot.service.dunge.service;

import azhukov.chatbot.service.dunge.ArtifactCatalog;
import azhukov.chatbot.service.dunge.DungeonTime;
import azhukov.chatbot.service.dunge.data.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DungeonEconomyService {
    private static final long HEAL_BASE_COST = 10;
    private static final long RUSH_COST = 40;
    private final HeroInfoService heroes;
    private final HeroHealthService health;
    private final BossService bosses;
    private final BossCombatService combat;
    private final ArtifactCatalog artifacts;
    private final DungeonJournalService journal;
    private final DungeonTime time;

    public long earnForEvent(HeroInfo hero) { return creditWithinDailyCap(hero, 1); }

    public long earnForFight(HeroInfo hero, long requestedCoins) { return creditWithinDailyCap(hero, requestedCoins); }

    public long fightCoinReward(long realDamage, long bossMaxHp) {
        long proportional = Math.min(4L, DungeonNumbers.ceilMultiplyDivide(Math.max(0, realDamage), 1000L, Math.max(1L, bossMaxHp)));
        return 1L + proportional;
    }

    private long creditWithinDailyCap(HeroInfo hero, long requested) {
        long cap = 15L + artifacts.dailyCoinCapBonus(hero);
        long available = Math.max(0L, cap - hero.getCoinsEarnedToday());
        long credited = Math.min(Math.max(0L, requested), available);
        hero.setCoins(DungeonNumbers.add(hero.getCoins(), credited));
        hero.setCoinsEarnedToday(DungeonNumbers.add(hero.getCoinsEarnedToday(), credited));
        if (credited > 0) DungeonMetrics.coinIncome(hero.getName(), hero.getCoins(), hero.getCoinsEarnedToday(), cap, credited);
        return credited;
    }

    public synchronized ActionResult heal(String name) {
        HeroInfo current = heroes.getCurrent(name);
        if (current == null) return ActionResult.fail("Герой не найден");
        long cost = Math.max(5L, HEAL_BASE_COST - artifacts.healDiscount(current));
        if (current.isDead()) return ActionResult.fail("Мёртвого героя нельзя лечить");
        if (current.getDamageGot() == HeroDamage.NONE) return ActionResult.fail("Герой уже здоров");
        if (current.isHealUsedToday()) return ActionResult.fail("Лечение уже использовано сегодня");
        if (current.getCoins() < cost) return ActionResult.fail("Недостаточно монет: нужно " + cost);
        DungeonOperation op = journal.prepare(DungeonOperation.Type.HEAL, name, bosses.getCurrentBoss().getInstanceId());
        op.setAmount(cost); journal.save(op);
        applyHeal(op);
        journal.complete(op);
        DungeonMetrics.economy("heal", name, heroes.getCurrent(name).getCoins(), cost);
        return ActionResult.ok("Лечение выполнено за " + cost + " монет");
    }

    public synchronized ActionResult rush(String name) {
        synchronized (bosses) {
            return rushLocked(name);
        }
    }

    private ActionResult rushLocked(String name) {
        HeroInfo hero = heroes.getCurrent(name);
        BossInfo boss = bosses.getCurrentBoss();
        if (hero == null || boss == null) return ActionResult.fail("Герой или босс не найден");
        if (hero.isDead()) return ActionResult.fail("Мёртвый герой не может совершить рывок");
        if (hero.isRushUsedToday()) return ActionResult.fail("Рывок уже использован сегодня");
        if (hero.getCoins() < RUSH_COST) return ActionResult.fail("Для рывка нужно 40 монет");
        int basisPoints = Math.min(350, 250 + artifacts.rushBasisPointsBonus(hero));
        long damage = Math.max(1L, DungeonNumbers.multiplyDivide(boss.getMaxHp(), basisPoints, 10_000L));
        DungeonOperation op = journal.prepare(DungeonOperation.Type.RUSH, name, boss.getInstanceId());
        op.setAmount(Math.min(damage, boss.getCurrentHp())).setSecondaryAmount(RUSH_COST); journal.save(op);
        applyEconomicDamage(op);
        journal.complete(op);
        DungeonMetrics.economy("rush", name, heroes.getCurrent(name).getCoins(), RUSH_COST);
        return ActionResult.ok("Рывок нанёс " + op.getAmount() + " урона");
    }

    public synchronized ActionResult siege(String name, long coins) {
        synchronized (bosses) {
            return siegeLocked(name, coins);
        }
    }

    private ActionResult siegeLocked(String name, long coins) {
        if (coins <= 0) return ActionResult.fail("Сумма должна быть положительной");
        HeroInfo hero = heroes.getCurrent(name);
        BossInfo boss = bosses.getCurrentBoss();
        if (hero == null || boss == null) return ActionResult.fail("Герой или босс не найден");
        if (hero.isDead()) return ActionResult.fail("Мёртвый герой не может управлять осадой");
        if (hero.getCoins() < coins) return ActionResult.fail("Недостаточно монет");
        int ppm = siegePpm(coins);
        long base;
        base = DungeonNumbers.multiplyDivide(boss.getMaxHp(), coins, ppm, 1_000_000L);
        long damage = Math.max(1L, base);
        damage = DungeonNumbers.add(damage, DungeonNumbers.multiplyDivide(damage, artifacts.siegePercentBonus(hero), 100L));
        DungeonOperation op = journal.prepare(DungeonOperation.Type.SIEGE, name, boss.getInstanceId());
        op.setAmount(Math.min(damage, boss.getCurrentHp())).setSecondaryAmount(coins); journal.save(op);
        applyEconomicDamage(op);
        journal.complete(op);
        DungeonMetrics.economy("siege", name, heroes.getCurrent(name).getCoins(), coins);
        return ActionResult.ok(siegeName(coins) + " нанёс " + op.getAmount() + " урона; пожертвовано " + coins);
    }

    private void applyEconomicDamage(DungeonOperation op) {
        heroes.update(op.getHero(), hero -> {
            if (hero.safeAppliedOperationIds().add(op.getId())) {
                hero.setCoins(Math.max(0L, hero.getCoins() - op.getSecondaryAmount()));
                if (op.getType() == DungeonOperation.Type.RUSH) hero.setRushUsedToday(true);
                if (op.getType() == DungeonOperation.Type.SIEGE) hero.setBossDonations(DungeonNumbers.add(hero.getBossDonations(), op.getSecondaryAmount()));
            }
        });
        if (op.getType() == DungeonOperation.Type.SIEGE) {
            bosses.updateCurrentBoss(boss -> {
                if (boss.safeAppliedOperationIds().add(op.getId() + ":treasury")) {
                    boss.setTreasury(DungeonNumbers.add(boss.getTreasury(), op.getSecondaryAmount()));
                    boss.safeDonationsByHero().merge(op.getHero(), op.getSecondaryAmount(), DungeonNumbers::add);
                }
            });
        }
        combat.applyPrepared(op);
    }

    public synchronized ActionResult ransom(String name, String artifactId) {
        HeroInfo current = heroes.getCurrent(name);
        if (current == null) return ActionResult.fail("Герой не найден");
        if (current.isDead()) return ActionResult.fail("Мёртвый герой не может выкупать трофеи");
        StolenArtifact stolen = current.safeStolenArtifacts().stream().filter(a -> a.getId().equals(artifactId)).findFirst().orElse(null);
        if (stolen == null) return ActionResult.fail("Такого трофея нет");
        long cost = 10L + 5L * Math.max(1, stolen.getRank());
        if (current.getCoins() < cost) return ActionResult.fail("Для выкупа нужно " + cost + " монет");
        DungeonOperation op = journal.prepare(DungeonOperation.Type.RANSOM, name, bosses.getCurrentBoss().getInstanceId());
        op.setAmount(cost).setArtifactId(artifactId); journal.save(op);
        applyRansom(op);
        journal.complete(op);
        DungeonMetrics.economy("ransom", name, heroes.getCurrent(name).getCoins(), cost);
        return ActionResult.ok("Артефакт выкуплен за " + cost + " монет");
    }

    public StolenArtifact stealRandom(HeroInfo hero, int chance, String source, azhukov.chatbot.service.dunge.DungeonRandom random) {
        String artifactId = planStealArtifactId(hero, chance, random);
        return stealById(hero, artifactId, source, time.now());
    }

    public String planStealArtifactId(HeroInfo hero, int chance, azhukov.chatbot.service.dunge.DungeonRandom random) {
        if (!random.chance(chance) || hero.safeOwnedArtifacts().isEmpty()) return null;
        return random.item(hero.safeOwnedArtifacts()).getId();
    }

    public StolenArtifact stealById(HeroInfo hero, String artifactId, String source, LocalDateTime stolenAt) {
        if (artifactId == null) return null;
        OwnedArtifact owned = hero.findArtifact(artifactId);
        if (owned == null) return null;
        hero.safeOwnedArtifacts().remove(owned);
        StolenArtifact stolen = new StolenArtifact(owned.getId(), owned.getRank(), source, stolenAt);
        hero.safeStolenArtifacts().add(stolen);
        DungeonMetrics.artifact("stolen", hero.getName(), stolen.getId(), stolen.getRank(), source);
        return stolen;
    }

    public void replayPending() {
        for (DungeonOperation op : journal.pending()) {
            if (op.getType() == DungeonOperation.Type.RUSH || op.getType() == DungeonOperation.Type.SIEGE) {
                journal.replay(op);
                applyEconomicDamage(op); journal.complete(op);
            } else if (op.getType() == DungeonOperation.Type.HEAL) {
                journal.replay(op);
                applyHeal(op); journal.complete(op);
            } else if (op.getType() == DungeonOperation.Type.RANSOM) {
                journal.replay(op);
                applyRansom(op); journal.complete(op);
            }
        }
    }

    private void applyHeal(DungeonOperation op) {
        heroes.update(op.getHero(), hero -> {
            if (hero.safeAppliedOperationIds().add(op.getId())) {
                hero.setCoins(Math.max(0L, hero.getCoins() - op.getAmount()));
                health.healOne(hero);
                hero.setHealUsedToday(true);
            }
        });
    }

    private void applyRansom(DungeonOperation op) {
        heroes.update(op.getHero(), hero -> {
            if (!hero.safeAppliedOperationIds().add(op.getId())) return;
            StolenArtifact item = hero.safeStolenArtifacts().stream()
                    .filter(a -> a.getId().equals(op.getArtifactId())).findFirst().orElse(null);
            if (item != null) {
                hero.setCoins(Math.max(0L, hero.getCoins() - op.getAmount()));
                hero.safeStolenArtifacts().remove(item);
                hero.addOwnedArtifact(new OwnedArtifact(item.getId(), item.getRank()));
                DungeonMetrics.artifact("ransomed", hero.getName(), item.getId(), item.getRank(), item.getSource());
            }
        });
    }

    public String treasury(String name) {
        HeroInfo hero = heroes.getCurrent(name); BossInfo boss = bosses.getCurrentBoss();
        if (hero == null || boss == null) return "Герой или босс не найден";
        List<java.util.Map.Entry<String, Long>> top = boss.safeDonationsByHero().entrySet().stream()
                .sorted(java.util.Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())).limit(10).toList();
        return "Монеты: " + hero.getCoins() + ", дневной заработок: " + hero.getCoinsEarnedToday() + "/" + (15 + artifacts.dailyCoinCapBonus(hero))
                + ", казна босса: " + boss.getTreasury() + (top.isEmpty() ? "" : ", вклад: " + top);
    }

    static int siegePpm(long coins) {
        if (coins >= 100) return 700;
        if (coins >= 50) return 600;
        if (coins >= 25) return 500;
        if (coins >= 10) return 450;
        return 400;
    }
    static String siegeName(long coins) {
        if (coins >= 100) return "Требушет";
        if (coins >= 50) return "Катапульта";
        if (coins >= 25) return "Баллиста";
        if (coins >= 10) return "Отряд лучников";
        return "Гоблин-лучник";
    }

    @Data
    public static class ActionResult {
        private final boolean success;
        private final String message;
        public static ActionResult ok(String message) { return new ActionResult(true, message); }
        public static ActionResult fail(String message) { return new ActionResult(false, message); }
    }
}
