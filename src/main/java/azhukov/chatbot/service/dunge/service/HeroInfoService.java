package azhukov.chatbot.service.dunge.service;

import azhukov.chatbot.service.ArticfactService;
import azhukov.chatbot.service.dunge.data.*;
import azhukov.chatbot.service.util.Randomizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.mutable.MutableInt;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
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
        });
        log.info("Heal complete. total: {}, dead: {}", counter.intValue(), toDelete.size());
        toDelete.forEach(store::delete);
    }

    public void addArtifact(String name, Artifact artifact) {
        update(name, heroInfo -> {
            if (heroInfo.getArtifacts() == null) {
                heroInfo.setArtifacts(new ArrayList<>());
            }
            heroInfo.getArtifacts().add(artifact);
        });
    }

    public void addArtifact(String name, String artifactId) {
        addArtifact(name, articfactService.getById(artifactId));
    }

}
