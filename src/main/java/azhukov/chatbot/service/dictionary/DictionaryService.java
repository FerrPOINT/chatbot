package azhukov.chatbot.service.dictionary;

import azhukov.chatbot.service.Randomizer;
import azhukov.chatbot.service.store.DailyStore;
import azhukov.chatbot.service.store.Store;
import azhukov.chatbot.service.users.UserCollectionStore;
import azhukov.chatbot.util.IOUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DictionaryService {

    private static final String DICTIONARY_KEY = "DICTIONARY";

    private final ObjectMapper objectMapper;
    private final DailyStore dailyStore;
    private final UserCollectionStore userCollectionStore;

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
                    dailyStore.getStore(DICTIONARY_KEY + "_" + dct.getId().toUpperCase()); // register store
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


        String result;
        if (key == null) {
            final List<String> keys = idToKeys.get(dictionary.getId());
            key = Randomizer.getRandomItem(keys);
            store.put(user, key);
            result = getDictionaryMessage(dictionary, key);
        } else {
            result = getDictionaryRepeatMessage(dictionary, key);
        }

        String collectPostfix = "";
        if (dictionary.isCollect()) {
            Set<String> currentSet = userCollectionStore.getCurrentSet(user, dictionary.getId());
            if (currentSet == null) {
                currentSet = new HashSet<>();
            }
            currentSet.add(key);
            userCollectionStore.save(user, currentSet, dictionary.getId());
            collectPostfix = getCollectionMessage(currentSet, dictionary);
        }

        return result + collectPostfix;
    }

    String getDictionaryMessage(Dictionary dictionary, String key) {
        return new StringJoiner(" ").add(dictionary.getPrefix()).add(dictionary.getData().get(key)).add(dictionary.getPostfix()).toString();
    }

    String getDictionaryRepeatMessage(Dictionary dictionary, String key) {
        return new StringJoiner(" ").add(dictionary.getRepeatPrefix()).add(dictionary.getData().get(key)).add(dictionary.getPostfix()).toString();
    }

    public String getCollectionMessage(Set<String> current, Dictionary dictionary) {
        return " У вас уже " + current.size() + " из " + dictionary.getData().size() + " : " + current.stream().sorted().collect(Collectors.joining(", "));
    }

}
