package azhukov.chatbot.service.dunge.service;

import azhukov.chatbot.service.dunge.ArticfactService;
import azhukov.chatbot.service.dunge.data.BossInfo;
import azhukov.chatbot.service.dunge.data.BossStore;
import azhukov.chatbot.util.IOUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
@Slf4j
public class BossService {

    private final ObjectMapper objectMapper;
    private final BossStore store;
    private final ArticfactService articfactService;

    private List<BossInfo> info;
    @Getter
    private final Set<String> oldRewards = Collections.newSetFromMap(new ConcurrentHashMap<>());

    //every 12 hours
    @Scheduled(cron = "0 * */12 ? * *")
    void update() {
        handlePrevBosses(bossInfo -> {
            if (bossInfo.getRewards() != null) {
                bossInfo.getRewards().stream()
                        .filter(s -> articfactService.getById(s) != null)
                        .forEach(oldRewards::add);
            }
        });
    }

    @PostConstruct
    synchronized void init() {
        try {
            info = new ArrayList<>();
            IOUtils.listFilesFromResources("dunge", "bosses.json", inputStream -> {
                try {
                    final List<BossInfo> infos = objectMapper.readValue(inputStream, new TypeReference<>() {
                    });
                    info.addAll(infos);
                    info.sort(Comparator.comparing(BossInfo::getStage));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception e) {
            throw new IllegalStateException("While reading dunge/boss", e);
        }
        update();
    }

    public synchronized void updateCurrentBoss(Consumer<BossInfo> consumer) {
        BossInfo currentBoss = getCurrentBoss();
        if (currentBoss != null) {
            consumer.accept(currentBoss);
            store.put("CURRENT_BOSS", currentBoss);
        }
    }

    public synchronized void resetCurrentBoss() {
        BossInfo currentBoss = getCurrentBoss();
        if (currentBoss != null) {
            setCurrentBoss(currentBoss.getStage());
        }
    }

    public synchronized BossInfo getCurrentBoss() {
        BossInfo bigBoss = store.get("CURRENT_BOSS");
        if (bigBoss == null) {
            store.put("CURRENT_BOSS", info.get(0));
            bigBoss = store.get("CURRENT_BOSS");
        } else if (bigBoss.isDead()) {
            bigBoss = next(bigBoss);
            if (bigBoss == null) {
                return null;
            }
            store.put("CURRENT_BOSS", bigBoss);
        }
        return bigBoss;
    }

    private synchronized BossInfo next(BossInfo bigBoss) {
        for (BossInfo iter : info) {
            if (iter.getStage() > bigBoss.getStage()) {
                store.put("BOSS_" + bigBoss.getStage(), bigBoss);
                bigBoss = iter;
                break;
            }
        }
        return bigBoss;
    }

    public synchronized BossInfo getBossInfo(int stage) {
        return info.get(stage - 1);
    }

    public synchronized void setCurrentBoss(int stage) {
        store.put("CURRENT_BOSS", info.get(stage - 1));
    }

    public synchronized BossInfo damage(String hero, int damage) {
        BossInfo bigBoss = store.get("CURRENT_BOSS");
        bigBoss.getDamagedHeroes().add(hero);
        bigBoss.setDamageReceived(bigBoss.getDamageReceived() + damage);
        store.put("CURRENT_BOSS", bigBoss);
        return bigBoss;
    }

    public String getCurrentBossData() {
        BossInfo currentBoss = getCurrentBoss();
        if (currentBoss == null) {
            return "Самый матёрый босс был побеждён, подождём пока не прийдёт кто-то посильнее! {DOGGIE}";
        }
        return currentBoss.isDead() ? "Босс уже отъехал" : new StringJoiner(", ")
                .add("Текущий босс - " + currentBoss.getName() + " - " + currentBoss.getLabel())
                .add("со своими приспешниками - " + currentBoss.getMinionsLabel())
                .add("силен против: " + (currentBoss.getStrong() == null ? "всех" : currentBoss.getStrong().getLabel()))
                .add(currentBoss.getWeak() == null ? "не имеет слабостей" : ("слаб против " + currentBoss.getWeak().getLabel()))
                .add("ХП: " + currentBoss.getCurrentHp() + " из " + currentBoss.getMaxHp())
                .toString();
    }

    public void handlePrevBosses(Consumer<BossInfo> consumer) {
        store.handleAll(bossInfo -> {
            if (bossInfo.isDead()) {
                consumer.accept(bossInfo);
            }
        });
    }

    public void reset() {
        store.clear();
    }

}
