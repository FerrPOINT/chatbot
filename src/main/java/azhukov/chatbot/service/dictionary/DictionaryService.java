package azhukov.chatbot.service.dictionary;

import azhukov.chatbot.service.Randomizer;
import azhukov.chatbot.service.store.DailyStore;
import azhukov.chatbot.service.store.Store;
import azhukov.chatbot.util.IOUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DictionaryService {

    private static final String DICTIONARY_KEY = "DICTIONARY";

    private final ObjectMapper objectMapper;
    private final DailyStore dailyStore;

    private final Map<String, Dictionary> commandsToDictionary = new HashMap<>();
    private final Map<String, Dictionary> idToDictionary = new HashMap<>();
    private final Map<String, List<String>> idToKeys = new HashMap<>();
    private final List<Dictionary> dictionaries = new ArrayList<>();

    @PostConstruct
    void init() {
        try {
            IOUtils.listFilesFromResources("dictionary", ".json", inputStream -> {
                try {
                    final Dictionary dct = objectMapper.readValue(inputStream, Dictionary.class);
                    dictionaries.add(dct);
                    if (idToDictionary.put(dct.getId(), dct) != null) {
                        throw new IllegalStateException("Duplicate id: " + dct.getId());
                    }

                    idToKeys.put(dct.getId(), new ArrayList<>(dct.getData().keySet()));
                    for (String command : dct.getCommandsList()) {
                        if (commandsToDictionary.put(command, dct) != null) {
                            throw new IllegalStateException("Duplicate command: " + command);
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            if (dictionaries.isEmpty()) {
                throw new IllegalStateException("dictionary is empty");
            }

            Collections.shuffle(dictionaries);
        } catch (Exception e) {
            throw new IllegalStateException("While reading dictionary", e);
        }
    }

    public String getDictionaryAnswer(String user, String messageLowerCase) {
        for (Dictionary dictionary : dictionaries) {
            for (String command : dictionary.getCommandsList()) {
                if (messageLowerCase.contains(command)) {
                    return getDictionaryMessage(user, dictionary);
                }
            }
        }
        return null;
    }

    public String getDictionaryMessage(String user, Dictionary dictionary) {
        final Store store = dailyStore.getStore(DICTIONARY_KEY + "_" + dictionary.getId().toUpperCase());
        String key = store.get(user);
        if (key == null) {
            final List<String> keys = idToKeys.get(dictionary.getId());
            key = Randomizer.getRandomItem(keys);
            store.put(user, key);
            return getDictionaryMessage(dictionary, key);
        } else {
            return getDictionaryRepeatMessage(dictionary, key);
        }
    }

    String getDictionaryMessage(Dictionary dictionary, String key) {
        return new StringJoiner(" ").add(dictionary.getPrefix()).add(dictionary.getData().get(key)).add(dictionary.getPostfix()).toString();
    }

    String getDictionaryRepeatMessage(Dictionary dictionary, String key) {
        return new StringJoiner(" ").add(dictionary.getRepeatPrefix()).add(dictionary.getData().get(key)).add(dictionary.getPostfix()).toString();
    }

}
