package azhukov.chatbot.service.dictionary;

import azhukov.chatbot.service.CommandsPermissionsService;
import azhukov.chatbot.service.store.DailyStore;
import azhukov.chatbot.service.store.Store;
import azhukov.chatbot.service.users.UserCollectionStore;
import azhukov.chatbot.service.util.Randomizer;
import azhukov.chatbot.util.IOUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

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
    private final CommandsPermissionsService commandsPermissionsService;

    private final Map<String, Dictionary> commandsToDictionary = new HashMap<>();
    private final Map<String, Dictionary> idToDictionary = new HashMap<>();
    private final Map<String, List<String>> idToKeys = new HashMap<>();
    @Getter
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
                    log.info("Dictionary request for {}", user);
                    if (dictionary.isLocked() && !commandsPermissionsService.isPermitted(command, user)) {
                        return "Вы пока еще не открыли эту команду {DOGGIE}";
                    }
                    return getDictionaryMessage(user, dictionary);
                }
            }
        }
        return null;
    }

    public String getDictionaryMessage(String user, Dictionary dictionary) {
        final Store store = dailyStore.getStore(DICTIONARY_KEY + "_" + dictionary.getId().toUpperCase());
        String key = store.get(user);
        boolean firstTimeToday = key == null;

        String result;
        Set<String> currentSet = null;
        if (firstTimeToday) {
            final List<String> keys = idToKeys.get(dictionary.getId());
            key = Randomizer.getRandomItem(keys);
            store.put(user, key);
            result = getDictionaryMessage(dictionary, key);
        } else {
            final Store secondary = dailyStore.getStore(DICTIONARY_KEY + "2_" + dictionary.getId().toUpperCase());
            String secondaryKey = secondary.get(user);
            if (secondaryKey == null && dictionary.isCollect()) {
                currentSet = userCollectionStore.getCurrentSet(user, dictionary.getId());
                if (CollectionUtils.isEmpty(currentSet)) {
                    final List<String> keys = idToKeys.get(dictionary.getId());
                    secondaryKey = Randomizer.getRandomItem(keys);
                    secondary.put(user, secondaryKey);
                    key = secondaryKey;
                    result = "Вы всё проиграли, но Доген даёт вам второй шанс " + getDictionaryMessage(dictionary, key);
                    firstTimeToday = true;
                } else {
                    result = getDictionaryRepeatMessage(dictionary, key);
                }
            } else {
                result = getDictionaryRepeatMessage(dictionary, key);
            }
        }

        String collectPostfix = "";
        if (dictionary.isCollect()) {
            if (currentSet == null) {
                currentSet = userCollectionStore.getCurrentSet(user, dictionary.getId());
            }
            if (currentSet == null) {
                currentSet = new HashSet<>();
            }
            String message;
            if (firstTimeToday) {
                message = currentSet.add(key) ? " Новьё." : " Повторка.";
            } else {
                message = currentSet.contains(key) ? " Забыл чтоли?" : " Лудоман.";
            }
            userCollectionStore.save(user, currentSet, dictionary.getId());
            collectPostfix = message + " " + (currentSet.size() == dictionary.getData().size() ? getFullCollectionMessage(dictionary) : getCollectionMessage(currentSet, dictionary));
        }

        return result + collectPostfix;
    }

    public Dictionary getById(String id) {
        return idToDictionary.get(id);
    }

    private String getDictionaryMessage(Dictionary dictionary, String key) {
        return new StringJoiner(" ").add(dictionary.getPrefix()).add(dictionary.getData().get(key)).add(dictionary.getPostfix()).toString();
    }

    private String getDictionaryRepeatMessage(Dictionary dictionary, String key) {
        return new StringJoiner(" ")
                .add(dictionary.getRepeatPrefix())
                .add(dictionary.isRepeatMessage() ? dictionary.getData().get(key) : key)
                .add(dictionary.getPostfix())
                .toString();
    }

    private String getCollectionMessage(Set<String> current, Dictionary dictionary) {
        return current.isEmpty() ? "Вы всё проиграли" : "У вас уже " + current.size() + " из " + dictionary.getData().size() + " : " + current.stream().sorted().collect(Collectors.joining(", "));
    }

    private String getFullCollectionMessage(Dictionary dictionary) {
        return dictionary.getFullCollectionMessage() != null ? dictionary.getFullCollectionMessage() : "Вся коллекция собрана, можно её !обменять";
    }

    public List<String> getTalismansList(String user) {
        Dictionary dictionary = getById("talisman");
        Set<String> currentSet = userCollectionStore.getCurrentSet(user, dictionary.getId());
        List<String> result = currentSet == null ? List.of() : new ArrayList<>(currentSet);
        if (!result.isEmpty()) {
            Collections.sort(result);
        }
        return result;
    }

}
