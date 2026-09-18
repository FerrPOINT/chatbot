package azhukov.chatbot.service.dunge;

import azhukov.chatbot.service.dunge.data.*;
import azhukov.chatbot.util.IOUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import azhukov.chatbot.service.dunge.service.DungeonNumbers;
import azhukov.chatbot.service.dunge.service.DungeonMetrics;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArtifactCatalog {
    public static final int MAX_RANK = 5;
    private static final Map<String, Integer> LATE_STAGE = Map.of(
            "kostebrak", 38, "mezza", 39, "goose", 40, "robin", 41, "kotovski", 42,
            "utan", 43, "kowak", 44, "grin43", 45, "char09", 46);

    private final ObjectMapper objectMapper;
    private final Map<String, Artifact> definitions = new HashMap<>();

    @PostConstruct
    public void init() {
        try {
            IOUtils.listFilesFromResources("artifacts", ".json", inputStream -> {
                try {
                    for (Artifact definition : objectMapper.readValue(inputStream, Artifact[].class)) {
                        if (definitions.put(definition.getId(), definition) != null) {
                            throw new IllegalStateException("Duplicate artifact id: " + definition.getId());
                        }
                    }
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            });
        } catch (Exception e) {
            throw new IllegalStateException("Unable to load artifact catalog", e);
        }
        if (definitions.isEmpty()) throw new IllegalStateException("Artifact catalog is empty");
    }

    public Artifact get(String id) {
        return definitions.get(id);
    }

    public List<Artifact> all() {
        return List.copyOf(definitions.values());
    }

    /** Returns unknown legacy ids; callers can expose them in a migration report. */
    public Set<String> migrateLegacy(HeroInfo hero) {
        Set<String> unknown = new LinkedHashSet<>();
        if (hero.getArtifacts() != null) {
            for (Artifact artifact : hero.getArtifacts()) {
                if (artifact == null || artifact.getId() == null || get(artifact.getId()) == null) {
                    unknown.add(artifact == null ? "<null>" : String.valueOf(artifact.getId()));
                    if (artifact != null && artifact.getId() != null && !hero.hasStolenArtifact(artifact.getId())) {
                        hero.addOwnedArtifact(new OwnedArtifact(artifact.getId(), 1));
                    }
                } else if (!hero.hasArtifact(artifact.getId()) && !hero.hasStolenArtifact(artifact.getId())) {
                    hero.addOwnedArtifact(new OwnedArtifact(artifact.getId(), 1));
                }
            }
            hero.setArtifacts(null);
        }
        hero.safeOwnedArtifacts().forEach(OwnedArtifact::normalize);
        normalizeOwnership(hero);
        return unknown;
    }

    private void normalizeOwnership(HeroInfo hero) {
        Map<String, StolenArtifact> stolen = new LinkedHashMap<>();
        for (StolenArtifact item : hero.safeStolenArtifacts()) {
            if (item != null && item.getId() != null) {
                item.setRank(Math.max(1, Math.min(MAX_RANK, item.getRank())));
                stolen.merge(item.getId(), item, (left, right) -> left.getRank() >= right.getRank() ? left : right);
            }
        }
        hero.setStolenArtifacts(new ArrayList<>(stolen.values()));
        Map<String, OwnedArtifact> owned = new LinkedHashMap<>();
        for (OwnedArtifact item : hero.safeOwnedArtifacts()) {
            if (item == null || item.getId() == null || stolen.containsKey(item.getId())) continue;
            item.normalize();
            owned.merge(item.getId(), item, (left, right) -> left.getRank() >= right.getRank() ? left : right);
        }
        hero.setOwnedArtifacts(new ArrayList<>(owned.values()));
    }

    public long attack(HeroInfo hero, BossInfo boss) {
        long value = Math.max(0L, hero.getLevel() * 10L);
        for (OwnedArtifact owned : hero.safeOwnedArtifacts()) {
            Artifact definition = get(owned.getId());
            if (definition == null || definition.getModifications() == null) continue;
            for (Modificator modification : definition.getModifications()) {
                if (modification.getModificationType() == ModificationType.ATTACK_CHANGE) {
                    value = modification.getValue() >= 0
                            ? DungeonNumbers.add(value, modification.getValue())
                            : Math.max(0L, value + modification.getValue());
                }
            }
        }
        for (OwnedArtifact owned : hero.safeOwnedArtifacts()) {
            Artifact definition = get(owned.getId());
            if (definition == null || definition.getModifications() == null) continue;
            for (Modificator modification : definition.getModifications()) {
                if (modification.getModificationType() == ModificationType.ATTACK_PERCENT) {
                    long delta = DungeonNumbers.multiplyDivide(value, Math.abs((long) modification.getValue()), 100L);
                    value = modification.getValue() >= 0 ? DungeonNumbers.add(value, delta) : Math.max(0L, value - delta);
                }
            }
        }
        if (boss != null && boss.getWeak() == hero.getType()) value = DungeonNumbers.add(value, value / 2L);
        return Math.max(0L, value);
    }

    public int dailyShield(HeroInfo hero) {
        int strongestPermanent = 0;
        for (OwnedArtifact owned : hero.safeOwnedArtifacts()) {
            Artifact definition = get(owned.getId());
            if (definition == null || definition.getModifications() == null) continue;
            for (Modificator modification : definition.getModifications()) {
                if (modification.getModificationType() == ModificationType.DAILY_GUARD) {
                    strongestPermanent = Math.max(strongestPermanent, modification.getValue());
                }
            }
        }
        return strongestPermanent + rank(hero, 42);
    }

    public int positiveXpPercent(HeroInfo hero) { return 5 * rank(hero, 39); }
    public int healDiscount(HeroInfo hero) { return rank(hero, 38); }
    public int deathHoursDiscount(HeroInfo hero) { return rank(hero, 40); }
    public int dailyCoinCapBonus(HeroInfo hero) { return rank(hero, 41); }
    public int rushBasisPointsBonus(HeroInfo hero) { return 20 * rank(hero, 43); }
    public int deathCoinRetentionPercent(HeroInfo hero) { return 5 * rank(hero, 44); }
    public int deathPenaltyReductionPoints(HeroInfo hero) { return rank(hero, 45); }
    public int siegePercentBonus(HeroInfo hero) { return 2 * rank(hero, 46); }

    public int rank(HeroInfo hero, int lateStage) {
        return LATE_STAGE.entrySet().stream()
                .filter(e -> e.getValue() == lateStage)
                .map(e -> hero.findArtifact(e.getKey()))
                .filter(Objects::nonNull)
                .mapToInt(OwnedArtifact::getRank).findFirst().orElse(0);
    }

    public RewardResult grantBossReward(HeroInfo hero, String id) {
        StolenArtifact stolen = hero.safeStolenArtifacts().stream()
                .filter(a -> Objects.equals(a.getId(), id)).findFirst().orElse(null);
        if (stolen != null) {
            hero.safeStolenArtifacts().remove(stolen);
            OwnedArtifact restored = new OwnedArtifact(id, Math.min(MAX_RANK, stolen.getRank() + 1));
            hero.addOwnedArtifact(restored);
            if (stolen.getRank() >= MAX_RANK) hero.setCoins(DungeonNumbers.add(hero.getCoins(), 25L));
            DungeonMetrics.artifact("boss_reward_restored", hero.getName(), id, restored.getRank(), "boss");
            return RewardResult.RESTORED_AND_RANKED;
        }
        OwnedArtifact owned = hero.findArtifact(id);
        if (owned == null) {
            hero.addOwnedArtifact(new OwnedArtifact(id, 1));
            DungeonMetrics.artifact("boss_reward_granted", hero.getName(), id, 1, "boss");
            return RewardResult.GRANTED;
        }
        if (owned.getRank() < MAX_RANK) {
            owned.setRank(owned.getRank() + 1);
            DungeonMetrics.artifact("boss_reward_ranked", hero.getName(), id, owned.getRank(), "boss");
            return RewardResult.RANKED;
        }
        hero.setCoins(DungeonNumbers.add(hero.getCoins(), 25L));
        DungeonMetrics.artifact("boss_reward_compensated", hero.getName(), id, owned.getRank(), "boss");
        return RewardResult.COMPENSATED;
    }

    public enum RewardResult { GRANTED, RANKED, RESTORED_AND_RANKED, COMPENSATED }
}
