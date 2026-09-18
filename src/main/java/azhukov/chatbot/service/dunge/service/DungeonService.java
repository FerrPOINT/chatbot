package azhukov.chatbot.service.dunge.service;

import azhukov.chatbot.dto.ChatRequest;
import azhukov.chatbot.service.combination.CombinationService;
import azhukov.chatbot.service.dunge.ArtifactCatalog;
import azhukov.chatbot.service.dunge.DungeonRandom;
import azhukov.chatbot.service.dunge.ability.HeroAbilityService;
import azhukov.chatbot.service.dunge.data.*;
import azhukov.chatbot.service.dunge.event.DungeEvent;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static azhukov.chatbot.service.dunge.data.HeroDamage.*;

@Service
@RequiredArgsConstructor
public class DungeonService {
    private static final int MAX_EVENTS_PER_DAY = 3;
    private final HeroInfoService heroes;
    private final HeroHealthService health;
    private final DungeonResetService reset;
    private final BossService bosses;
    private final BossCombatService combat;
    private final DungeonEconomyService economy;
    private final DungeonStateService state;
    private final DungeonJournalService journal;
    private final ArtifactCatalog artifacts;
    private final DungeonRandom random;
    private final List<DungeEvent> events;
    private final CombinationService combinations;
    private final HeroAbilityService abilities;

    public synchronized String getDungeonResponse(ChatRequest request) {
        String name = request.getUserName();
        HeroInfo hero = heroes.getCurrent(name);
        if (hero == null) {
            hero = heroes.createNew(name);
            touch(hero.getName());
            return "Новый герой — " + hero.getName() + " " + hero.getType().getLabel() + ". Используйте !данж, !стата, !босс";
        }
        reset.reviveIfExpired(name);
        hero = heroes.getCurrent(name);
        try {
            if (hero.isDead()) return "Вы мертвы до " + health.reviveAt(hero) + ". Способность Некроманта может вернуть вас раньше.";
            BossInfo boss = bosses.getCurrentBoss();
            if (boss == null || boss.isDead()) return "Данж готовит следующего босса.";
            if (shouldRunEvent(hero)) return runEvent(hero);
            return fight(hero);
        } finally {
            touch(hero.getName());
        }
    }

    private boolean shouldRunEvent(HeroInfo hero) {
        return hero.getEvents() == 0 || (hero.getEvents() < MAX_EVENTS_PER_DAY && random.nextInt(1, 11) < 5 - hero.getEvents());
    }

    private String runEvent(HeroInfo hero) {
        DungeEvent event = weightedEvent();
        DungeonOperation op = journal.prepare(DungeonOperation.Type.EVENT, hero.getName(), bosses.getCurrentBoss().getInstanceId());
        op.setPayload(event.getClass().getName());
        journal.save(op);
        final String[] message = {""};
        heroes.update(hero.getName(), current -> {
            if (current.safeAppliedOperationIds().add(op.getId())) {
                economy.earnForEvent(current);
                message[0] = event.handle(current);
                current.setEvents(Math.min(MAX_EVENTS_PER_DAY, current.getEvents() + 1));
            }
        });
        journal.complete(op);
        return combinations.getRandomCombinationMessage("dunge-prefix") + " " + message[0] + " {DOGGIE}";
    }

    public synchronized void replayPendingActions() {
        for (DungeonOperation operation : journal.pending()) {
            if (operation.getType() == DungeonOperation.Type.EVENT) {
                journal.replay(operation);
                DungeEvent event = events.stream().filter(e -> e.getClass().getName().equals(operation.getPayload())).findFirst().orElse(null);
                if (event != null) {
                    heroes.update(operation.getHero(), hero -> {
                        if (hero.safeAppliedOperationIds().add(operation.getId())) {
                            economy.earnForEvent(hero);
                            event.handle(hero);
                            hero.setEvents(Math.min(MAX_EVENTS_PER_DAY, hero.getEvents() + 1));
                        }
                    });
                }
                journal.complete(operation);
            } else if (operation.getType() == DungeonOperation.Type.FIGHT) {
                journal.replay(operation);
                state.consumeBuffs(operation.getId());
                combat.applyPrepared(operation);
                applyFightHero(operation);
                journal.complete(operation);
            }
        }
    }

    private DungeEvent weightedEvent() {
        int total = events.stream().mapToInt(e -> e.getWeight().getValue()).sum();
        int point = random.nextInt(0, total);
        for (DungeEvent event : events) {
            point -= event.getWeight().getValue();
            if (point < 0) return event;
        }
        return events.get(events.size() - 1);
    }

    private String fight(HeroInfo snapshot) {
        synchronized (bosses) {
            return fightLocked(snapshot, bosses.getCurrentBoss());
        }
    }

    private String fightLocked(HeroInfo snapshot, BossInfo boss) {
        DungeonOperation operation = journal.prepare(DungeonOperation.Type.FIGHT, snapshot.getName(), boss.getInstanceId());
        GlobalBuffs buffs = state.peekBuffs();
        FightPlan plan = planFight(snapshot, boss, buffs);
        long requested = boss.getImmunity() == snapshot.getType() ? 0L : plan.attackDamage;
        long realDamage = Math.min(Math.max(0L, requested), boss.getCurrentHp());
        int receivedDamage = Math.max(0, plan.incomingDamage - Math.max(0, snapshot.getShield()) - plan.temporaryShield);
        long xp = DungeonCombatMath.normalizedXp(realDamage, snapshot.getLevel(), plan.rounds,
                receivedDamage, boss.getStage(), buffs.expPercent(), artifacts.positiveXpPercent(snapshot));
        operation.setAmount(realDamage).setSecondaryAmount(xp)
                .setCoinReward(economy.fightCoinReward(realDamage, boss.getMaxHp()))
                .setIncomingDamage(plan.incomingDamage).setTemporaryShield(plan.temporaryShield).setRounds(plan.rounds)
                .setAttackBuffPercent(buffs.attackPercent()).setExperienceBuffPercent(buffs.expPercent()).setShieldBuff(buffs.shield())
                .setArtifactId(economy.planStealArtifactId(snapshot, boss.getStealPercent(), random))
                .setPayload("BOSS_" + boss.getInstanceId());
        journal.save(operation);
        state.consumeBuffs(operation.getId());
        BossCombatService.DamageResult hit = combat.applyPrepared(operation);
        FightHeroOutcome outcome = applyFightHero(operation);
        journal.complete(operation);
        HeroInfo updated = heroes.getCurrent(snapshot.getName());
        StringBuilder result = new StringBuilder("Данж: ").append(snapshot.getName()).append(" против ").append(boss.getName())
                .append("; раундов ").append(plan.rounds)
                .append("; урон ").append(hit.getRealDamage())
                .append(boss.getImmunity() == snapshot.getType() ? " (иммунитет босса)" : "")
                .append("; получено опыта ").append(xp)
                .append("; травма ").append(updated.getDamageGot().getStatus())
                .append("; щит ").append(updated.getShield());
        if (outcome.stolen != null) result.append("; украден артефакт ").append(outcome.stolen.getId());
        if (outcome.injury != null && outcome.injury.isDied()) result.append("; PRESS F, возвращение в ").append(health.reviveAt(updated));
        if (hit.isKilled()) result.append("; БОСС ПОБЕЖДЁН, награды выданы ровно один раз");
        else result.append("; HP босса ").append(bosses.getCurrentBoss().getCurrentHp());
        return result.append(" {DOGGIE}").toString();
    }

    private FightHeroOutcome applyFightHero(DungeonOperation operation) {
        final HeroHealthService.DamageResult[] injury = new HeroHealthService.DamageResult[1];
        final StolenArtifact[] stolen = new StolenArtifact[1];
        heroes.update(operation.getHero(), hero -> {
            if (!hero.safeAppliedOperationIds().add(operation.getId() + ":hero")) return;
            hero.addExp(operation.getSecondaryAmount());
            economy.earnForFight(hero, operation.getCoinReward());
            int tempAbsorbed = Math.min(operation.getTemporaryShield(), operation.getIncomingDamage());
            injury[0] = health.damage(hero, operation.getIncomingDamage() - tempAbsorbed, operation.getCreatedAt());
            stolen[0] = economy.stealById(hero, operation.getArtifactId(), operation.getPayload(), operation.getCreatedAt());
        });
        return new FightHeroOutcome(injury[0], stolen[0]);
    }

    private FightPlan planFight(HeroInfo hero, BossInfo boss, GlobalBuffs buffs) {
        int temporaryShield = buffs.shield();
        int projectedShield = Math.max(0, hero.getShield()) + temporaryShield;
        int currentDamage = hero.getDamageGot() == null ? 0 : hero.getDamageGot().getValue();
        boolean risky = currentDamage - projectedShield > MEDIUM.getValue();
        int rounds = 0;
        int incoming = 0;
        long attack = artifacts.attack(hero, boss);
        long totalAttack = 0;
        int crit = Math.max(1, (int) hero.getCrit() + 1);
        if (risky) {
            rounds = 1;
            incoming = randomDamage(boss.getStrong() == hero.getType());
            totalAttack = DungeonNumbers.multiply(attack, crit + 1L);
        } else {
            while (currentDamage + incoming - projectedShield < BIG.getValue() && rounds < 32) {
                incoming += randomDamage(boss.getStrong() == hero.getType());
                rounds++;
                totalAttack = DungeonNumbers.add(totalAttack, DungeonNumbers.multiply(attack, crit));
            }
        }
        totalAttack = DungeonNumbers.add(totalAttack, DungeonNumbers.multiplyDivide(totalAttack, buffs.attackPercent(), 100L));
        return new FightPlan(Math.max(1, rounds), incoming, temporaryShield, Math.max(0L, totalAttack));
    }

    private int randomDamage(boolean bossStrong) {
        return random.nextInt(bossStrong ? 2 : 1, HUGE.getValue());
    }

    public String useHeroAbility(ChatRequest request) {
        HeroInfo actor = heroes.getCurrent(request.getUserName());
        if (actor == null) return "Герой не найден.";
        HeroInfo previous = state.previousExisting(actor.getName(), heroes);
        try { return abilities.useHeroAbility(actor, previous); }
        finally { touch(actor.getName()); }
    }

    public void touch(String heroName) {
        HeroInfo hero = heroes.getCurrent(heroName);
        String canonical = hero == null ? heroName.toLowerCase(Locale.ROOT).trim() : hero.getName();
        state.update(s -> s.touchHero(canonical));
    }

    public String getHeroStats(ChatRequest request) {
        HeroInfo hero = heroes.getCurrent(request.getUserName());
        if (hero == null) return "Герой не найден";
        BossInfo boss = bosses.getCurrentBoss();
        String death = hero.isDead() ? ", возвращение: " + health.reviveAt(hero) : "";
        long healCost = Math.max(5L, 10L - artifacts.healDiscount(hero));
        boolean healingAvailable = !hero.isDead() && hero.getDamageGot() != NONE && !hero.isHealUsedToday() && hero.getCoins() >= healCost;
        boolean rushAvailable = !hero.isDead() && !hero.isRushUsedToday() && hero.getCoins() >= 40L;
        return hero.getName() + ", класс: " + hero.getType().getLabel() + ", травма: " + hero.getDamageGot().getStatus()
                + ", щит: " + hero.getShield() + ", риск смерти в следующем бою: " + deathRisk(hero, boss) + "%"
                + (boss != null && boss.getStrong() == hero.getType() ? ", босс усилен против вашего класса" : "")
                + ", способность: " + (!hero.isSpecialAbilityUsed() && !hero.isDead() ? "доступна" : "недоступна")
                + ", лечение: " + (healingAvailable ? "доступно" : "недоступно")
                + ", рывок: " + (rushAvailable ? "доступен" : "недоступен")
                + ", уровень: " + hero.getLevel() + ", опыт: " + hero.getExperience()
                + ", монеты: " + hero.getCoins() + ", лимит: " + hero.getCoinsEarnedToday() + "/" + (15 + artifacts.dailyCoinCapBonus(hero)) + death;
    }

    private String deathRisk(HeroInfo hero, BossInfo boss) {
        if (hero.isDead() || boss == null) return hero.isDead() ? "100.0" : "0.0";
        int origin = boss.getStrong() == hero.getType() ? 2 : 1;
        int outcomes = Math.max(1, HUGE.getValue() - origin);
        int deadly = 0;
        int availableShield = Math.max(0, hero.getShield()) + state.peekBuffs().shield();
        for (int damage = origin; damage < HUGE.getValue(); damage++) {
            int applied = Math.max(0, damage - availableShield);
            if (hero.getDamageGot().getValue() + applied >= DEAD.getValue()) deadly++;
        }
        double risk = deadly * 100.0 / outcomes;
        risk *= (100.0 - Math.max(0, Math.min(100, hero.getRebornPercentage()))) / 100.0;
        return String.format(Locale.ROOT, "%.1f", risk);
    }

    public String getHeroInfo(ChatRequest request) {
        HeroInfo hero = heroes.getCurrent(request.getUserName());
        return hero == null ? "Герой не найден" : hero.getType().getAbilityName() + " — " + hero.getType().getAbilityDescription();
    }

    public String getArtifactsMessage(ChatRequest request) {
        HeroInfo hero = heroes.getCurrent(request.getUserName());
        if (hero == null) return "Герой не найден";
        if (hero.safeOwnedArtifacts().isEmpty()) return "У вас нет артефактов";
        return hero.safeOwnedArtifacts().stream().map(a -> {
            Artifact definition = artifacts.get(a.getId());
            return (definition == null ? a.getId() : definition.getName()) + " (ранг " + a.getRank() + ")";
        }).collect(Collectors.joining(", ", "Ваши артефакты: ", ""));
    }

    public String getStolenArtifacts(ChatRequest request) {
        HeroInfo hero = heroes.getCurrent(request.getUserName());
        if (hero == null) return "Герой не найден";
        if (hero.safeStolenArtifacts().isEmpty()) return "Украденных артефактов нет";
        return hero.safeStolenArtifacts().stream().map(a -> a.getId() + " (ранг " + a.getRank() + ", выкуп " + (10 + 5 * a.getRank()) + ")")
                .collect(Collectors.joining(", ", "Украденные трофеи: ", ""));
    }

    public String getContribution() {
        BossInfo boss = bosses.getCurrentBoss();
        if (boss == null) return "Босс не найден";
        Set<String> names = new HashSet<>(boss.safeDamageByHero().keySet());
        names.addAll(boss.safeDonationsByHero().keySet());
        return names.stream().sorted((a, b) -> Long.compare(boss.safeDamageByHero().getOrDefault(b, 0L), boss.safeDamageByHero().getOrDefault(a, 0L)))
                .limit(20).map(n -> n + ": урон " + boss.safeDamageByHero().getOrDefault(n, 0L) + ", пожертвования " + boss.safeDonationsByHero().getOrDefault(n, 0L))
                .collect(Collectors.joining(" | ", "Вклад в " + boss.getInstanceId() + ": ", ""));
    }

    public String getLadderResponse() {
        List<Pair<String, Long>> top = heroes.getTopLevel(20);
        StringJoiner joiner = new StringJoiner(", ");
        for (int i = 0; i < top.size(); i++) joiner.add((i + 1) + ". " + top.get(i).getLeft() + " " + top.get(i).getRight() + "lvl");
        return "Топ героев: " + joiner;
    }

    public String getHeroesListResponse() {
        return HeroClass.VALUES.stream().map(c -> c.getLabel() + " — " + c.getAbilityName())
                .collect(Collectors.joining(" | ", "Классы: ", " {DOGGIE}"));
    }

    public DungeonEconomyService.ActionResult heal(ChatRequest request) { try { return economy.heal(request.getUserName()); } finally { touch(request.getUserName()); } }
    public DungeonEconomyService.ActionResult rush(ChatRequest request) { try { return economy.rush(request.getUserName()); } finally { touch(request.getUserName()); } }
    public DungeonEconomyService.ActionResult siege(ChatRequest request, long amount) { try { return economy.siege(request.getUserName(), amount); } finally { touch(request.getUserName()); } }
    public DungeonEconomyService.ActionResult ransom(ChatRequest request, String id) { try { return economy.ransom(request.getUserName(), id); } finally { touch(request.getUserName()); } }
    public String treasury(ChatRequest request) { touch(request.getUserName()); return economy.treasury(request.getUserName()); }

    private record FightPlan(int rounds, int incomingDamage, int temporaryShield, long attackDamage) {}
    private record FightHeroOutcome(HeroHealthService.DamageResult injury, StolenArtifact stolen) {}
}
