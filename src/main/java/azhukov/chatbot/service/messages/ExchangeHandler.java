package azhukov.chatbot.service.messages;

import azhukov.chatbot.dto.ChatRequest;
import azhukov.chatbot.dto.ChatResponse;
import azhukov.chatbot.service.dictionary.Dictionary;
import azhukov.chatbot.service.dictionary.DictionaryService;
import azhukov.chatbot.service.users.UserCollectionStore;
import azhukov.chatbot.service.util.Randomizer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

@Component
@AllArgsConstructor
public class ExchangeHandler extends MessageHandler {

    private final UserCollectionStore userCollectionStore;
    private final DictionaryService dictionaryService;

    @Override
    public ChatResponse answerMessage(ChatRequest message, String text, String lowerCase) {
        if (lowerCase.startsWith("!обменять")) {
            if (lowerCase.contains("!обменять сейлор на призму")) {
                // TODO
            }
            if (lowerCase.contains("!обменять талисманы на призму")) {
                // TODO
            }

            if (lowerCase.contains("!обменять талисманы на сейлор")) {
                Dictionary talisman = dictionaryService.getById("talisman");
                Set<String> talismansSet = userCollectionStore.getCurrentSet(message.getUserName(), talisman.getId());
                Dictionary sailor = dictionaryService.getById("sailor");
                Set<String> sailorSet = userCollectionStore.getCurrentSet(message.getUserName(), sailor.getId());
                if (sailorSet != null && sailorSet.size() == sailor.getData().size()) {
                    return createUserMessage(message, "Вы уже собрали сейлор!");
                }
                if (talismansSet != null && talismansSet.size() == talisman.getData().size()) {
                    if (sailorSet == null) {
                        sailorSet = new HashSet<>();
                    }
                    ArrayList<String> itemsToRoll = new ArrayList<>(sailor.getData().keySet());
                    itemsToRoll.removeAll(sailorSet);
                    String randomItem = Randomizer.getRandomItem(itemsToRoll);
                    sailorSet.add(randomItem);
                    userCollectionStore.save(message.getUserName(), sailorSet, sailor.getId());
                    userCollectionStore.save(message.getUserName(), Set.of(), talisman.getId());
                    return createUserMessage(message, "Обмен произведен. Вы поменяли всю коллекцию талисманов на знак " + sailor.getData().get(randomItem) + " {DOGGIE}");
                }
                return createUserMessage(message, "Вы еще не собрали всей коллекции. Собирайте и сражайтесь на арене!");
            }
            return createUserMessage(message, "На данный момент можно обменять всю коллекцию талисманов на один из недостающих знаков Сейлор (!обменять талисманы на сейлор)");
        }
        return null;
    }
}
