package azhukov.chatbot.service.dunge.ability;

import azhukov.chatbot.service.dunge.ArtifactCatalog;
import azhukov.chatbot.service.dunge.DungeonRandom;
import azhukov.chatbot.service.dunge.data.*;
import azhukov.chatbot.service.dunge.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HeroAbilityService {
    private static final long PLANNED_AND_USABLE = 1L;
    private final HeroInfoService heroes;
    private final HeroHealthService health;
    private final DungeonStateService state;
    private final BossService bosses;
    private final BossCombatService combat;
    private final ArtifactCatalog artifacts;
    private final DungeonRandom random;
    private final DungeonJournalService journal;

    public synchronized String useHeroAbility(HeroInfo actor, HeroInfo previous) {
        synchronized (bosses) {
            return useHeroAbilityLocked(actor, previous);
        }
    }

    private String useHeroAbilityLocked(HeroInfo actor, HeroInfo previous) {
        if (actor.isDead()) return "Мёртвый герой не может использовать способность.";
        if (actor.isSpecialAbilityUsed()) return "Способность уже использована сегодня.";
        HeroClass effective = actor.getType() == HeroClass.WEREWOLF ? randomOtherClass() : actor.getType();
        BossInfo boss = bosses.getCurrentBoss();
        DungeonOperation operation = journal.prepare(DungeonOperation.Type.ABILITY, actor.getName(), boss == null ? null : boss.getInstanceId());
        operation.setAmount(effective.ordinal()).setTargetHero(previous == null ? null : previous.getName());
        String planMessage = plan(operation, actor, previous, effective, boss);
        journal.save(operation);
        String result = operation.getSecondaryAmount() == PLANNED_AND_USABLE
                ? applyPlanned(operation, false)
                : planMessage;
        journal.complete(operation);
        return actor.getType() == HeroClass.WEREWOLF
                ? actor.getType().getAbilityName() + " → " + effective.getLabel() + ". " + result
                : result;
    }

    private String plan(DungeonOperation op, HeroInfo actor, HeroInfo target, HeroClass type, BossInfo boss) {
        String error = switch (type) {
            case FAIRY -> target == null || target.isDead() || target.getDamageGot() == HeroDamage.NONE ? "нет подходящего живого раненого героя" : null;
            case NECRO -> target == null || !target.isDead() ? "предыдущий герой не мёртв"
                    : target.getLevel() >= actor.getLevel() ? "цель должна быть ниже уровнем" : null;
            case SAMURAI -> boss == null || boss.isDead() ? "живой босс не найден" : null;
            case ROGUE -> target == null || !target.hasArtifacts() ? "у предыдущего героя нет доступных артефактов" : null;
            default -> null;
        };
        if (error != null) {
            op.setSecondaryAmount(0).setPayload(error);
            return type.getAbilityName() + ": " + error;
        }
        op.setSecondaryAmount(PLANNED_AND_USABLE);
        if (type == HeroClass.ROGUE) {
            if (!random.chance(50)) op.setPayload("ROGUE_FAIL");
            else op.setArtifactId(random.item(target.safeOwnedArtifacts()).getId()).setPayload("ROGUE_COPY");
        }
        return type.getAbilityName();
    }

    private String applyPlanned(DungeonOperation op, boolean replay) {
        HeroClass type = HeroClass.values()[(int) op.getAmount()];
        HeroInfo actor = heroes.getCurrent(op.getHero());
        HeroInfo target = op.getTargetHero() == null ? null : heroes.getCurrent(op.getTargetHero());
        if (actor == null) return type.getAbilityName() + ": герой не найден";
        String message = switch (type) {
            case SAILOR -> applyBuff(op, type, "атака", s -> s.buffs().addAttack());
            case SHAMAN -> applyBuff(op, type, "щит", s -> s.buffs().addShield());
            case NOBLE -> applyBuff(op, type, "опыт", s -> s.buffs().addExp());
            case DEFENDER -> {
                updateOnce(actor.getName(), op.getId() + ":effect", h -> h.addShields(2));
                yield type.getAbilityName() + ": добавлено 2 единицы щита";
            }
            case FAIRY -> {
                if (target != null) updateOnce(target.getName(), op.getId() + ":target", h -> health.heal(h, 2));
                yield type.getAbilityName() + ": " + op.getTargetHero() + " исцелён на две ступени";
            }
            case NECRO -> {
                if (target != null) updateOnce(target.getName(), op.getId() + ":target", health::reviveEarly);
                yield type.getAbilityName() + ": " + op.getTargetHero() + " возвращён со средней травмой";
            }
            case PRISONER -> {
                updateOnce(actor.getName(), op.getId() + ":effect", h -> h.setRebornPercentage(50));
                yield type.getAbilityName() + ": следующая смертельная проверка имеет ровно 50% спасения";
            }
            case SAMURAI -> {
                synchronized (bosses) {
                    BossInfo boss = bosses.getCurrentBoss();
                    long damage = boss == null ? 0L : artifacts.attack(actor, boss);
                    BossCombatService.DamageResult hit = combat.damageWithId(op.getId() + ":boss-hit", actor.getName(), damage, DungeonOperation.Type.ABILITY_DAMAGE);
                    yield type.getAbilityName() + ": безответный удар нанёс " + hit.getRealDamage();
                }
            }
            case ROGUE -> applyRogue(op, actor);
            case WEREWOLF -> throw new IllegalStateException("Werewolf must be resolved while preparing the operation");
        };
        updateOnce(actor.getName(), op.getId(), h -> h.setSpecialAbilityUsed(true));
        return replay ? type.getAbilityName() + ": незавершённая операция восстановлена" : message;
    }

    private String applyBuff(DungeonOperation op, HeroClass type, String label, java.util.function.Consumer<DungeonState> effect) {
        final int[] value = {0};
        state.update(s -> {
            if (s.safeAppliedOperationIds().add(op.getId())) effect.accept(s);
            GlobalBuffs buffs = s.buffs();
            value[0] = type == HeroClass.SAILOR ? buffs.attackPercent()
                    : type == HeroClass.SHAMAN ? buffs.shield() : buffs.expPercent();
        });
        DungeonMetrics.buff("added", label, value[0], op.getId());
        return type.getAbilityName() + ": глобальный бонус «" + label + "» добавлен для следующего боя";
    }

    private String applyRogue(DungeonOperation op, HeroInfo actor) {
        if ("ROGUE_FAIL".equals(op.getPayload())) return HeroClass.ROGUE.getAbilityName() + ": копирование не удалось";
        final boolean[] duplicate = {false};
        updateOnce(actor.getName(), op.getId() + ":effect", hero -> {
            duplicate[0] = hero.hasArtifact(op.getArtifactId()) || hero.hasStolenArtifact(op.getArtifactId());
            if (duplicate[0]) hero.addExp(1000);
            else hero.addOwnedArtifact(new OwnedArtifact(op.getArtifactId(), 1));
        });
        return HeroClass.ROGUE.getAbilityName() + (duplicate[0] ? ": дубликат превращён в 1000 опыта" : ": скопирован артефакт ранга 1");
    }

    private void updateOnce(String heroName, String operationId, java.util.function.Consumer<HeroInfo> mutation) {
        heroes.update(heroName, hero -> { if (hero.safeAppliedOperationIds().add(operationId)) mutation.accept(hero); });
    }

    private HeroClass randomOtherClass() {
        List<HeroClass> choices = Arrays.stream(HeroClass.values()).filter(c -> c != HeroClass.WEREWOLF).toList();
        return random.item(choices);
    }

    public synchronized void replayPending() {
        for (DungeonOperation operation : journal.pending()) {
            if (operation.getType() == DungeonOperation.Type.ABILITY) {
                journal.replay(operation);
                if (operation.getSecondaryAmount() == PLANNED_AND_USABLE) applyPlanned(operation, true);
                journal.complete(operation);
            }
        }
    }
}
