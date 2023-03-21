package azhukov.chatbot.service.dunge.service;

import azhukov.chatbot.service.dunge.data.BossInfo;
import azhukov.chatbot.service.dunge.data.BossStore;
import azhukov.chatbot.util.IOUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.StringJoiner;

@Service
@RequiredArgsConstructor
@Slf4j
public class BossService {

    private final ObjectMapper objectMapper;
    private final BossStore store;

    private List<BossInfo> info;

    @PostConstruct
    void init() {
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
    }

    public synchronized BossInfo getCurrentBoss() {
        BossInfo bigBoss = store.get("BIG_BOSS");
        if (bigBoss == null) {
            store.put("BIG_BOSS", info.get(0));
            bigBoss = store.get("BIG_BOSS");
        }
        return bigBoss;
    }

    public synchronized BossInfo next(BossInfo bossInfo) {
        BossInfo bigBoss = store.get("BIG_BOSS");
        for (BossInfo iter : info) {
            if (iter.getStage() > bigBoss.getStage()) {
                bigBoss = iter;
                store.put("BIG_BOSS", bigBoss);
                break;
            }
        }
        return bigBoss;
    }

    public synchronized BossInfo damage(String hero, int damage) {
        BossInfo bigBoss = store.get("BIG_BOSS");
        bigBoss.getDamagedHeroes().add(hero);
        bigBoss.setGotDamage(bigBoss.getGotDamage() + damage);
        store.put("BIG_BOSS", bigBoss);
        return bigBoss;
    }

    public String getCurrentBossData() {
        BossInfo currentBoss = getCurrentBoss();
        return new StringJoiner(", ")
                .add("Текущий босс - " + currentBoss.getName() + " " + currentBoss.getLabel())
                .add("со своими преспешниками - " + currentBoss.getMinionsLabel())
                .add("силен против: " + currentBoss.getStrong().getLabel())
                .add("слаб против " + currentBoss.getWeak().getLabel())
                .add(currentBoss.isDead() ? "Босс уже отъехал" : "ХП: " + currentBoss.getCurrentHp() + " из " + currentBoss.getMaxHp())
                .toString();
    }

    public void reset() {
        store.clear();
    }

}
