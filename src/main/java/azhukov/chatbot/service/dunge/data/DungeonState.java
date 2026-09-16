package azhukov.chatbot.service.dunge.data;

import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

@Data
public class DungeonState {
    private LocalDate lastDungeonResetDate;
    private List<String> heroHistory = new ArrayList<>();
    private GlobalBuffs nextFightBuffs = new GlobalBuffs();
    private Map<String, List<String>> migrationUnknownArtifacts = new LinkedHashMap<>();
    private Set<String> appliedOperationIds = new HashSet<>();

    public void touchHero(String name) {
        if (name == null) return;
        if (heroHistory == null) heroHistory = new ArrayList<>();
        heroHistory.removeIf(name::equals);
        heroHistory.add(name);
    }

    public GlobalBuffs buffs() {
        if (nextFightBuffs == null) nextFightBuffs = new GlobalBuffs();
        return nextFightBuffs;
    }

    public Set<String> safeAppliedOperationIds() {
        if (appliedOperationIds == null) appliedOperationIds = new HashSet<>();
        return appliedOperationIds;
    }
}
