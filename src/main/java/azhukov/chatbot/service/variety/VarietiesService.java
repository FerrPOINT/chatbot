package azhukov.chatbot.service.variety;

import azhukov.chatbot.service.Randomizer;
import azhukov.chatbot.service.store.DailyStore;
import azhukov.chatbot.service.store.Store;
import azhukov.chatbot.util.IOUtils;
import azhukov.chatbot.util.Range;
import azhukov.chatbot.util.RangesContainer;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class VarietiesService {

    private static final String STORE_KEY = "VARIETIES";

    private final ObjectMapper objectMapper;
    private final DailyStore dailyStore;

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
                    String storeKey = user + "-" + variety.getId();
                    final String percent = store.get(storeKey);
                    int percentNumber;
                    if (percent == null) {
                        percentNumber = Randomizer.nextInt(101);
                        store.put(storeKey, String.valueOf(percentNumber));
                    } else {
                        percentNumber = Integer.parseInt(percent);
                    }
                    Variety item = container.getItem(percentNumber);
                    return item == null ? null : "Вы на " + percentNumber + "% " + variety.getName() + ". " + item.getMessage();
                }
            }
        }
        return null;
    }

}
