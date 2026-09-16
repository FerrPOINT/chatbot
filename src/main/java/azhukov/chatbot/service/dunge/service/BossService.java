package azhukov.chatbot.service.dunge.service;

import azhukov.chatbot.service.dunge.ArtifactCatalog;
import azhukov.chatbot.service.dunge.DungeonTime;
import azhukov.chatbot.service.dunge.data.BossInfo;
import azhukov.chatbot.service.dunge.data.BossStore;
import azhukov.chatbot.service.dunge.data.HeroClass;
import azhukov.chatbot.service.store.StoreUpdater;
import azhukov.chatbot.util.IOUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class BossService {
    private static final String CURRENT = "CURRENT_BOSS";
    private final ObjectMapper objectMapper;
    private final BossStore store;
    private final ArtifactCatalog artifacts;
    private final DungeonTime time;
    private List<BossInfo> definitions;
    @Getter private final Set<String> oldRewards = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @PostConstruct
    public synchronized void init() {
        definitions = new ArrayList<>();
        try {
            IOUtils.listFilesFromResources("dunge", "bosses.json", input -> {
                try { definitions.addAll(objectMapper.readValue(input, new TypeReference<List<BossInfo>>() {})); }
                catch (Exception e) { throw new IllegalStateException(e); }
            });
        } catch (Exception e) {
            throw new IllegalStateException("Unable to load dungeon bosses", e);
        }
        definitions.sort(Comparator.comparingInt(BossInfo::getStage));
        if (definitions.isEmpty()) throw new IllegalStateException("Boss catalog is empty");
        migrateArchives();
        migrateCurrent();
        refreshOldRewards();
    }

    private void migrateArchives() {
        store.updateAll(boss -> {
            if (boss.getCycle() < 1) boss.setCycle(1);
            if (boss.getStoredMaxHp() <= 0 && boss.getStage() > 0) boss.setMaxHp(baseHp(boss.getStage()));
            normalizeBoss(boss);
        }, new StoreUpdater<>(key -> {
            if (key.matches("BOSS_\\d+")) return "BOSS_1_" + key.substring("BOSS_".length());
            return key;
        }, Comparator.comparingLong(BossInfo::getDamageReceived)));
        store.commit();
    }

    private void migrateCurrent() {
        BossInfo current = store.get(CURRENT);
        if (current == null) {
            store.put(CURRENT, createInstance(1, 1));
            store.commit();
            return;
        }
        if (current.getCycle() < 1) current.setCycle(1);
        if (current.getStoredMaxHp() <= 0) current.setMaxHp(baseHp(current.getStage()));
        if (current.getStartedAt() == null) {
            current.setStartedAt(time.now());
            current.setStartedAtEstimated(true);
        }
        normalizeBoss(current);
        store.put(CURRENT, current);
        store.commit();
    }

    public synchronized BossInfo getCurrentBoss() { return store.get(CURRENT); }

    public synchronized void updateCurrentBoss(Consumer<BossInfo> consumer) {
        BossInfo boss = getCurrentBoss();
        if (boss != null) { consumer.accept(boss); store.put(CURRENT, boss); store.commit(); }
    }

    public synchronized BossInfo transitionAfterVictory() {
        BossInfo defeated = getCurrentBoss();
        if (defeated == null || !defeated.isDead()) return defeated;
        store.put(archiveKey(defeated), defeated);
        defeated.safeRewards().stream().filter(id -> artifacts.get(id) != null).forEach(oldRewards::add);
        int nextStage = defeated.getStage() < 46 ? defeated.getStage() + 1 : 38;
        int nextCycle = defeated.getStage() < 46 ? defeated.getCycle()
                : (int) Math.min(Integer.MAX_VALUE, (long) defeated.getCycle() + 1L);
        BossInfo next = createInstance(nextStage, nextCycle);
        store.put(CURRENT, next);
        store.commit();
        return next;
    }

    public synchronized BossInfo createInstance(int stage, int cycle) {
        BossInfo definition = definition(stage);
        BossInfo result = objectMapper.convertValue(definition, BossInfo.class);
        result.setCycle(Math.max(1, cycle));
        result.setDamageReceived(0);
        result.setTreasury(0);
        result.setRewardsGranted(false);
        result.setStartedAt(time.now());
        result.setStartedAtEstimated(false);
        long base = baseHp(stage);
        long factor = DungeonNumbers.add(100L, DungeonNumbers.multiply(15L, result.getCycle() - 1L));
        result.setMaxHp(Math.max(1L, DungeonNumbers.multiplyDivide(base, factor, 100L)));
        result.setDamagedHeroes(new HashSet<>());
        result.setDamageByHero(new HashMap<>());
        result.setDonationsByHero(new HashMap<>());
        result.setAppliedOperationIds(new HashSet<>());
        return result;
    }

    private long baseHp(int stage) { return Math.max(1L, (long) definition(stage).getLevel() * 1000L); }

    private void normalizeBoss(BossInfo boss) {
        boss.setDamageReceived(Math.max(0L, Math.min(boss.getMaxHp(), boss.getDamageReceived())));
        boss.setTreasury(Math.max(0L, boss.getTreasury()));
        boss.setNormalDamage(Math.max(0L, boss.getNormalDamage()));
        boss.setAbilityDamage(Math.max(0L, boss.getAbilityDamage()));
        boss.setRushDamage(Math.max(0L, boss.getRushDamage()));
        boss.setSiegeDamage(Math.max(0L, boss.getSiegeDamage()));
        boss.safeDamagedHeroes(); boss.safeRewards(); boss.safeAppliedOperationIds();
        boss.safeDamageByHero().replaceAll((name, value) -> Math.max(0L, value == null ? 0L : value));
        boss.safeDonationsByHero().replaceAll((name, value) -> Math.max(0L, value == null ? 0L : value));
    }

    private BossInfo definition(int stage) {
        return definitions.stream().filter(b -> b.getStage() == stage).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown boss stage: " + stage));
    }

    public String getCurrentBossData() {
        BossInfo boss = getCurrentBoss();
        if (boss == null) return "Данж временно недоступен {DOGGIE}";
        StringJoiner result = new StringJoiner(", ")
                .add("Босс " + boss.getName() + " — " + boss.getLabel())
                .add("этап " + boss.getStage()).add("цикл " + boss.getCycle())
                .add("возраст " + (boss.getStartedAt() == null ? "неизвестен" : Duration.between(boss.getStartedAt(), time.now()).toHours() + "ч"))
                .add("HP " + boss.getCurrentHp() + "/" + boss.getMaxHp())
                .add("участников " + boss.safeDamagedHeroes().size())
                .add("обычный урон " + boss.getNormalDamage())
                .add("урон способностей " + boss.getAbilityDamage())
                .add("урон рывков " + boss.getRushDamage())
                .add("осадный урон " + boss.getSiegeDamage()).add("казна " + boss.getTreasury())
                .add("силён против " + (boss.getStrong() == null ? "никого" : boss.getStrong().getLabel()))
                .add(boss.getWeak() == null ? "без слабости" : "слаб против " + boss.getWeak().getLabel());
        return result.toString();
    }

    public void handlePrevBosses(Consumer<BossInfo> consumer) {
        store.handleAll(b -> { if (b.isDead() && !Objects.equals(b.getInstanceId(), getCurrentBoss().getInstanceId())) consumer.accept(b); });
    }

    private void refreshOldRewards() {
        store.handleAll(b -> { if (b.isDead()) b.safeRewards().stream().filter(id -> artifacts.get(id) != null).forEach(oldRewards::add); });
    }

    private String archiveKey(BossInfo boss) { return "BOSS_" + boss.getCycle() + "_" + boss.getStage(); }
    public synchronized void compactAppliedOperationIds(Set<String> retainedIds) {
        store.updateAll(boss -> boss.safeAppliedOperationIds().removeIf(id -> retainedIds.stream().noneMatch(active -> id.equals(active) || id.startsWith(active + ":"))));
        store.commit();
    }
}
