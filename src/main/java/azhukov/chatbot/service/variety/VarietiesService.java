package azhukov.chatbot.service.variety;

import azhukov.chatbot.service.CommandsPermissionsService;
import azhukov.chatbot.service.dunge.ArticfactService;
import azhukov.chatbot.service.dunge.data.Artifact;
import azhukov.chatbot.service.dunge.service.HeroInfoService;
import azhukov.chatbot.service.store.DailyStore;
import azhukov.chatbot.service.store.Store;
import azhukov.chatbot.service.util.Randomizer;
import azhukov.chatbot.util.IOUtils;
import azhukov.chatbot.util.Range;
import azhukov.chatbot.util.RangesContainer;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VarietiesService {

    private static final String STORE_KEY = "VARIETIES";
    private static final int TOP_ENTRIES_MAX_SIZE = 3;

    private final ObjectMapper objectMapper;
    private final DailyStore dailyStore;
    private final CommandsPermissionsService commandsPermissionsService;
    private final ArticfactService articfactService;
    private final HeroInfoService heroInfoService;

    private final List<VarietyList> varieties = new ArrayList<>();
    private final Map<String, RangesContainer<Variety>> ranges = new HashMap<>();

    @PostConstruct
    void init() {
        dailyStore.getStore(STORE_KEY); // register store
        try {
            Set<String> ids = new HashSet<>();
            IOUtils.listFilesFromResources("variety", ".json", inputStream -> {
                try {
                    final VarietyList variety = objectMapper.readValue(inputStream, VarietyList.class);
                    this.varieties.add(variety);
                    final List<Range<Variety>> ranges = new ArrayList<>();
                    variety.getVarieties().forEach(v -> ranges.add(parse(v)));
                    this.ranges.put(variety.getId(), new RangesContainer<>(ranges));
                    if (!ids.add(variety.getId())) {
                        throw new IllegalStateException("Duplicate id: " + variety.getId());
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            if (this.varieties.isEmpty()) {
                throw new IllegalStateException("varieties are empty");
            }

            Collections.shuffle(this.varieties);
        } catch (Exception e) {
            throw new IllegalStateException("While reading varieties", e);
        }
    }

    private Range<Variety> parse(Variety variety) {
        String range = variety.getRange();
        int start;
        int end;
        if (range.contains("-")) {
            String[] tokenized = range.split("-");
            start = Integer.parseInt(tokenized[0]);
            end = Integer.parseInt(tokenized[1]);
        } else {
            start = end = Integer.parseInt(range);
        }
        return new Range<>(start, end, variety);
    }

    public String getVarietyMessage(String user, String lowerMessage) {
        if (lowerMessage == null) {
            return null;
        }
        final Store store = dailyStore.getStore(STORE_KEY);
        for (VarietyList variety : varieties) {
            for (String command : variety.getCommands()) {
                RangesContainer<Variety> container = ranges.get(variety.getId());
                if (lowerMessage.contains(command)) {
                    if (lowerMessage.contains(command + " топ")) {
                        return getTop(variety);
                    }
                    if (variety.isLocked() && !commandsPermissionsService.isPermitted(command, user)) {
                        return "Вы пока еще не открыли эту команду";
                    }
                    String storeKey = user + "-" + variety.getId();
                    final String storedPercent = store.get(storeKey);
                    int percentNumber;
                    if (storedPercent == null) {
                        percentNumber = Randomizer.nextInt(101);
                        store.put(storeKey, String.valueOf(percentNumber));
                    } else {
                        percentNumber = Integer.parseInt(storedPercent);
                    }
                    Variety item = container.getItem(percentNumber);
                    return item == null ? null : "Вы на " + percentNumber + "% " + variety.getName() + ". " + item.getMessage() + getVarietyMessage(user, variety, storedPercent == null ? percentNumber : 0);
                }
            }
        }
        return null;
    }

    String getVarietyMessage(String user, VarietyList list, int percent) {
        if (percent >= 95) {
            List<Artifact> arts = new ArrayList<>();
            switch (list.getId()) {
                case "sytkin" -> arts.add(articfactService.getById("sytkin-guitar"));
                case "tolerancy" -> arts.add(articfactService.getById("tolerancy-item"));
            }
            if (!arts.isEmpty()) {
                heroInfoService.update(user, heroInfo -> arts.forEach(heroInfo::addArtifact));
                return " В данже у вас появляется: " + arts.stream().map(Artifact::getName).collect(Collectors.joining(", "));
            }
        }
        return "";
    }

    public String getTop(VarietyList variety) {
        String varietyPostfix = "-" + variety.getId();
        final Store store = dailyStore.getStore(STORE_KEY);
        Map<String, Integer> map = new HashMap<>();
        store.foreach(stringStringEntry -> {
            String key = stringStringEntry.getKey();
            if (!key.endsWith(varietyPostfix)) {
                return;
            }
            Integer value = Integer.parseInt(stringStringEntry.getValue());
            map.put(key.substring(0, key.length() - varietyPostfix.length()), value);
        });

        if (map.isEmpty()) {
            return "Никто не " + variety.getName();
        }

        ArrayList<Map.Entry<String, Integer>> entries = new ArrayList<>(map.entrySet());

        entries.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));

        StringJoiner result = new StringJoiner(", ");

        for (int i = 0; i < Math.min(TOP_ENTRIES_MAX_SIZE, entries.size()); i++) {
            Map.Entry<String, Integer> entry = entries.get(i);
            result.add(entry.getValue() + "%" + entry.getKey());
        }

        return variety.getName() + " ТОП - " + result;
    }

}
