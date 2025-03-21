package azhukov.chatbot.service.dunge.service;

import azhukov.chatbot.service.dunge.ArticfactService;
import azhukov.chatbot.service.dunge.data.*;
import azhukov.chatbot.service.store.StoreUpdater;
import azhukov.chatbot.service.util.Randomizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
@Service
public class HeroInfoService {

    private final HeroInfoStore store;
    private final ArticfactService articfactService;


    public HeroInfo getCurrent(String name) {
        return store.get(name);
    }

    public void update(HeroInfo info, Consumer<HeroInfo> updater) {
        update(info.getName(), updater);
    }

    public void update(String name, Consumer<HeroInfo> updater) {
        HeroInfo info = getOrCreateNew(name);
        updater.accept(info);
        store.put(name, info);
    }

    public void dead(String name) {
        update(name, heroInfo -> heroInfo.setDeadTime(LocalDateTime.now()));
    }

    public void clean(String name) {
        store.put(name, null);
    }

    public HeroInfo createNew(String name) {
        HeroClass[] values = HeroClass.values();
        HeroInfo heroInfo = new HeroInfo()
                .setName(name)
                .setDamageGot(HeroDamage.NONE)
                .setType(Stream.of(values).skip(Randomizer.nextInt(values.length)).findFirst().get());
        store.put(name, heroInfo);
        return heroInfo;
    }

    private HeroInfo getOrCreateNew(String name) {
        HeroInfo heroInfo = getCurrent(name);
        if (heroInfo == null) {
            heroInfo = createNew(name);
        }
        return heroInfo;
    }

    public void reset() {
        store.clear();
    }

    public void healAll() {
        log.info("Start to heal all");
        List<String> toDelete = new ArrayList<>();
        log.info("Start to heal all");
        MutableInt counter = new MutableInt();
        store.updateAll(heroInfo -> {
            counter.increment();
            if (heroInfo.getDeadTime() != null) {
                toDelete.add(heroInfo.getName());
            }
            heroInfo.setDamageGot(HeroDamage.NONE);
            heroInfo.resetShield();
            heroInfo.setCrit(0);
            heroInfo.setEvents(0);
            heroInfo.setSpecialAbilityUsed(false);
            heroInfo.setRebornPercentage(0);
        });
        log.info("Heal complete. total: {}, dead: {}", counter.intValue(), toDelete.size());
        toDelete.forEach(store::delete);
    }

    public void addArtifact(String name, Artifact artifact) {
        update(name, heroInfo -> heroInfo.addArtifact(artifact));
    }

    public void addArtifact(String name, String artifactId) {
        addArtifact(name, articfactService.getById(artifactId));
    }

    public void distinctAllArtifacts() {
        HashMap<String, Artifact> arts = new HashMap<>();
        store.updateAll(heroInfo -> {
            arts.clear();
            List<Artifact> artifacts = heroInfo.getArtifacts();
            if (artifacts != null) {
                for (Artifact artifact : artifacts) {
                    Artifact byId = articfactService.getById(artifact.getId());
                    if (byId != null) {
                        arts.put(byId.getId(), byId);
                    }
                }
                heroInfo.setArtifacts(new ArrayList<>(arts.values()));
            }
        });
    }

    public void updateAll(Consumer<HeroInfo> acceptor) {
        store.updateAll(acceptor);
    }

    public List<Pair<String, Integer>> getTopLevel(int count) {
        List<Pair<String, Integer>> result = new ArrayList<>();
        store.handleAll(heroInfo -> result.add(Pair.of(heroInfo.getName(), heroInfo.getLevel())));
        return result.stream().sorted((o1, o2) -> Integer.compare(o2.getRight(), o1.getRight())).limit(count).collect(Collectors.toList());
    }

    public void migrateCaseSensitive() {
        store.updateAll(
                heroInfo -> heroInfo.setName(heroInfo.getName().toLowerCase()),
                new StoreUpdater<>(String::toLowerCase, Comparator.comparingInt(HeroInfo::getExperience))
        );
    }


}
