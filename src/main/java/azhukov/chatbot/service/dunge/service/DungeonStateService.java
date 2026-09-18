package azhukov.chatbot.service.dunge.service;

import azhukov.chatbot.service.dunge.data.DungeonState;
import azhukov.chatbot.service.dunge.data.DungeonStateStore;
import azhukov.chatbot.service.dunge.data.GlobalBuffs;
import azhukov.chatbot.service.dunge.data.HeroInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DungeonStateService {
    private static final String KEY = "GLOBAL";
    private final DungeonStateStore store;

    public synchronized DungeonState get() {
        DungeonState state = store.get(KEY);
        if (state == null) {
            state = new DungeonState();
            store.put(KEY, state);
            store.commit();
        }
        return state;
    }

    public synchronized void update(Consumer<DungeonState> updater) {
        DungeonState state = get();
        updater.accept(state);
        store.put(KEY, state);
        store.commit();
    }

    public synchronized GlobalBuffs peekBuffs() {
        GlobalBuffs source = get().buffs();
        GlobalBuffs copy = new GlobalBuffs();
        copy.setAttackStacks(source.getAttackStacks());
        copy.setShieldStacks(source.getShieldStacks());
        copy.setExpStacks(source.getExpStacks());
        return copy;
    }

    public synchronized void consumeBuffs(String operationId) {
        update(s -> {
            if (s.safeAppliedOperationIds().add(operationId + ":buffs")) {
                GlobalBuffs buffs = s.buffs();
                DungeonMetrics.buff("consumed", "attack_percent", buffs.attackPercent(), operationId);
                DungeonMetrics.buff("consumed", "shield", buffs.shield(), operationId);
                DungeonMetrics.buff("consumed", "experience_percent", buffs.expPercent(), operationId);
                s.setNextFightBuffs(new GlobalBuffs());
            }
        });
    }

    public synchronized HeroInfo previousExisting(String currentName, HeroInfoService heroes) {
        List<String> history = get().getHeroHistory();
        if (history == null) return null;
        for (int i = history.size() - 1; i >= 0; i--) {
            String candidate = history.get(i);
            HeroInfo hero = heroes.getCurrent(candidate);
            if (hero != null && !hero.getName().equals(currentName)) return hero;
        }
        return null;
    }

    public synchronized void compactAppliedOperationIds(Set<String> retainedIds) {
        update(s -> s.safeAppliedOperationIds().removeIf(id -> !DungeonAppliedOperationCompactor.isRetained(id, retainedIds)));
    }
}
