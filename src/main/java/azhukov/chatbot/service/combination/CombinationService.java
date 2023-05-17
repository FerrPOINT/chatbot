package azhukov.chatbot.service.combination;

import azhukov.chatbot.service.store.DailyStore;
import azhukov.chatbot.service.util.Randomizer;
import azhukov.chatbot.util.IOUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.text.StrBuilder;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CombinationService {

    private final ObjectMapper objectMapper;
    private final DailyStore dailyStore;

    private final Map<String, Combination> idToItem = new HashMap<>();
    @Getter
    private final List<Combination> items = new ArrayList<>();

    @PostConstruct
    void init() {
        try {
            IOUtils.listFilesFromResources("combination", ".json", inputStream -> {
                try {
                    final Combination data = objectMapper.readValue(inputStream, Combination.class);
                    if ((data.getDynamicParts().size() + 1) != data.getStaticParts().size()) {
                        throw new IllegalStateException("Illegal combination: " + data.getId());
                    }
                    items.add(data);
                    if (idToItem.put(data.getId(), data) != null) {
                        throw new IllegalStateException("Duplicate id: " + data.getId());
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            if (items.isEmpty()) {
                throw new IllegalStateException("Items are empty");
            }
            Collections.shuffle(items);
        } catch (Exception e) {
            throw new IllegalStateException("While reading items", e);
        }
    }

    public Combination getById(String id) {
        return idToItem.get(id);
    }

    public String getCombinationMessage(String user, String lowerCase) {
        for (Combination item : items) {
            for (String command : item.getCommands()) {
                if (lowerCase.startsWith(command)) {
//                    Store combination = dailyStore.getStore("COMBINATION");
//                    String todayValue = combination.get(user);
//                    if (todayValue == null) {
//                        todayValue = createRandomValue(item);
//                        combination.put(user, todayValue);
//                    }
//                    return todayValue;
                    return createRandomValue(item);
                }
            }
        }
        return null;
    }

    public String getRandomCombinationMessage(String id) {
        Combination combination = idToItem.get(id);
        return combination == null ? null : createRandomValue(combination);
    }

    private String createRandomValue(Combination combination) {
        List<String> staticParts = combination.getStaticParts();
        List<List<String>> dynamicParts = combination.getDynamicParts();
        StrBuilder sb = new StrBuilder();
        for (int i = 0; i < staticParts.size(); i++) {
            sb.append(staticParts.get(i));
            if (i < dynamicParts.size()) {
                List<String> parts = dynamicParts.get(i);
                sb.append(Randomizer.getRandomItem(parts));
            }
        }
        return sb.toString();
    }

}
