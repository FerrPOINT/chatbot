package azhukov.chatbot.service.messages;

import azhukov.chatbot.dto.ChatRequest;
import azhukov.chatbot.dto.ChatResponse;
import azhukov.chatbot.service.ArticfactService;
import azhukov.chatbot.service.dictionary.Dictionary;
import azhukov.chatbot.service.dictionary.DictionaryService;
import azhukov.chatbot.service.dunge.data.Artifact;
import azhukov.chatbot.service.dunge.service.HeroInfoService;
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
    private final ArticfactService articfactService;
    private final HeroInfoService heroInfoService;

    @Override
    public ChatResponse answerMessage(ChatRequest message, String text, String lowerCase) {
        if (lowerCase.startsWith("!обменять")) {
            if (lowerCase.contains("!обменять сейлор на призму")) {
                Dictionary sailor = dictionaryService.getById("sailor");
                Set<String> sailorSet = userCollectionStore.getCurrentSet(message.getUserName(), sailor.getId());
                if (sailorSet != null && sailor.getData().size() == sailorSet.size()) {
                    Artifact prism = articfactService.getById("prism");
                    heroInfoService.addArtifact(message.getUserName(), prism);
                    userCollectionStore.save(message.getUserName(), Set.of(), sailor.getId());
                    return createUserMessage(message, "Ты получаешь призму: +" + prism.getModifications().get(0).getValue() + " к атаке");
                }
                return createUserMessage(message, "Вы еще не собрали всей коллекции. Собирайте и сражайтесь на арене, меняйтесь!");
            }
//            if (lowerCase.contains("!обменять талисманы на ???")) {
//                // TODO
//            }

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
            return createUserMessage(message, "!обменять талисманы на сейлор, !обменять сейлор на призму");
        }
        return null;
    }
}
