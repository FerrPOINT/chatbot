package azhukov.chatbot.service.dunge.service;

import azhukov.chatbot.service.dunge.ArtifactCatalog;
import azhukov.chatbot.service.dunge.DungeonRandom;
import azhukov.chatbot.service.dunge.data.*;
import azhukov.chatbot.service.store.StoreUpdater;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.util.*;
import java.time.LocalDateTime;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HeroInfoService {
    private final HeroInfoStore store;
    private final ArtifactCatalog artifacts;
    private final DungeonRandom random;
    private final DungeonJournalService journal;

    public synchronized HeroInfo getCurrent(String name) {
        if (name == null) return null;
        String key = normalize(name);
        HeroInfo hero = store.get(key);
        if (hero != null && normalize(hero)) {
            store.put(key, hero);
            store.commit();
        }
        return hero;
    }

    public synchronized void update(String name, Consumer<HeroInfo> updater) {
        String key = normalize(name);
        HeroInfo info = getOrCreateNew(key);
        updater.accept(info);
        normalize(info);
        store.put(key, info);
        store.commit();
    }

    public synchronized HeroInfo createNew(String name) {
        String key = normalize(name);
        HeroClass[] values = HeroClass.values();
        HeroInfo hero = new HeroInfo().setName(key).setDamageGot(HeroDamage.NONE)
                .setType(values[random.nextInt(0, values.length)]);
        store.put(key, hero);
        store.commit();
        return hero;
    }

    private HeroInfo getOrCreateNew(String name) {
        HeroInfo hero = getCurrent(name);
        return hero == null ? createNew(name) : hero;
    }

    public synchronized void updateAll(Consumer<HeroInfo> acceptor) {
        store.updateAll(hero -> { normalize(hero); acceptor.accept(hero); });
        store.commit();
    }

    public synchronized List<HeroInfo> all() {
        List<HeroInfo> result = new ArrayList<>();
        store.handleAll(hero -> { normalize(hero); result.add(hero); });
        return result;
    }

    public List<Pair<String, Long>> getTopLevel(int count) {
        return all().stream().map(h -> Pair.of(h.getName(), h.getLevel()))
                .sorted((a, b) -> Long.compare(b.getRight(), a.getRight()))
                .limit(count).collect(Collectors.toList());
    }

    public synchronized void migrateCaseSensitive() {
        store.updateAll(hero -> hero.setName(normalize(hero.getName())),
                new StoreUpdater<>(HeroInfoService::normalize, Comparator.comparingLong(HeroInfo::getExperience)));
        store.commit();
    }

    public synchronized MigrationReport migrateAll() {
        MigrationReport report = new MigrationReport();
        store.updateAll(hero -> {
            report.heroes++;
            Set<String> unknown = artifacts.migrateLegacy(hero);
            if (!unknown.isEmpty()) report.unknownArtifacts.put(hero.getName(), unknown);
            normalize(hero);
        });
        store.commit();
        return report;
    }

    private boolean normalize(HeroInfo hero) {
        boolean changed = false;
        if (hero.getDamageGot() == null) { hero.setDamageGot(HeroDamage.NONE); changed = true; }
        if (hero.getType() == null) { hero.setType(HeroClass.DEFENDER); changed = true; }
        if (hero.getExperience() < 0) { hero.setExperience(0); changed = true; }
        if (hero.getCoins() < 0) { hero.setCoins(0); changed = true; }
        if (hero.getCoinsEarnedToday() < 0) { hero.setCoinsEarnedToday(0); changed = true; }
        if (hero.getBossDamage() < 0) { hero.setBossDamage(0); changed = true; }
        if (hero.getBossDonations() < 0) { hero.setBossDonations(0); changed = true; }
        if (hero.getShield() < 0) { hero.setShield(0); changed = true; }
        if (hero.getEvents() < 0 || hero.getEvents() > 3) { hero.setEvents(Math.max(0, Math.min(3, hero.getEvents()))); changed = true; }
        if (!Float.isFinite(hero.getCrit()) || hero.getCrit() < 0) { hero.setCrit(0); changed = true; }
        if (hero.getDeadTime() != null && hero.getDamageGot() != HeroDamage.DEAD) { hero.setDamageGot(HeroDamage.DEAD); changed = true; }
        if (hero.getDamageGot() == HeroDamage.DEAD && hero.getDeadTime() == null) { hero.setDeadTime(LocalDateTime.now()); changed = true; }
        if (hero.getOwnedArtifacts() == null || hero.getStolenArtifacts() == null || hero.getAppliedOperationIds() == null || hero.getArtifacts() != null) {
            artifacts.migrateLegacy(hero);
            hero.safeOwnedArtifacts(); hero.safeStolenArtifacts(); hero.safeAppliedOperationIds();
            changed = true;
        }
        return changed;
    }

    private static String normalize(String name) { return name.toLowerCase(Locale.ROOT).trim(); }

    public void addArtifact(String name, Artifact artifact) {
        if (artifact == null) return;
        DungeonOperation operation = journal.prepare(DungeonOperation.Type.ARTIFACT_GRANT, name, null);
        operation.setArtifactId(artifact.getId());
        journal.save(operation);
        applyArtifactGrant(operation);
        journal.complete(operation);
    }

    public void addArtifact(String name, String artifactId) {
        Artifact artifact = artifacts.get(artifactId);
        if (artifact != null) addArtifact(name, artifact);
    }

    public void distinctAllArtifacts() { migrateAll(); }

    private void applyArtifactGrant(DungeonOperation operation) {
        update(operation.getHero(), hero -> {
            if (hero.safeAppliedOperationIds().add(operation.getId())) {
                Artifact artifact = artifacts.get(operation.getArtifactId());
                if (artifact != null) hero.addArtifact(artifact);
            }
        });
    }

    public void replayPendingArtifactGrants() {
        for (DungeonOperation operation : journal.pending()) {
            if (operation.getType() == DungeonOperation.Type.ARTIFACT_GRANT) {
                journal.replay(operation);
                applyArtifactGrant(operation);
                journal.complete(operation);
            }
        }
    }

    @lombok.Data
    public static class MigrationReport {
        private int heroes;
        private Map<String, Set<String>> unknownArtifacts = new LinkedHashMap<>();
    }
}
