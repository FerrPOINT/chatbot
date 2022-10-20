package azhukov.chatbot.service.messages;

import azhukov.chatbot.constants.Constants;
import azhukov.chatbot.dto.ChatRequest;
import azhukov.chatbot.dto.ChatResponse;
import azhukov.chatbot.service.store.DailyStore;
import azhukov.chatbot.service.util.Randomizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DoggieHandler extends MessageHandler {

    private static String MASTER_NAME = Constants.MASTER_NAME;
    private static List<String> MASTER_MESSAGES = List.of("Аффьььь {DOGGIE}", "ррряф {DOGGIE}", "Вуфь? {DOGGIE}", "тяфф из-за угла {DOGGIE}", "аф-кусь {DOGGIE}", "ну допустим вуф {DOGGIE}");
    private static List<String> MASTER_PERSONAL_MESSAGES = List.of("авууу {DOGGIE} :love:", "!любовь {DOGGIE}", "афь афь :love: (ластится)", "вуфь, кусь за зёпку {DOGGIE}");

    @Autowired
    private DailyStore dailyStore;

    @Override
    public ChatResponse answerMessage(ChatRequest message, String text, String lowerCase) {
        if (lowerCase.contains("!гроши")) {
            return createUserMessage(message, "ваши гроши забрал Доген {DOGGIE}");
        }
        if (lowerCase.contains("!ауф")) {
            return createUserMessage(message, "Все мои догги делают ауф {DOGGIE}");
        }
        if (lowerCase.contains("!трусики")) {
            return createUserMessage(message, (Randomizer.tossCoin() ? "Вы посмотрели на трусики {PANTS}" : "Вам не удалось посмотреть на трусики {PLEASURE}"));
        }
        if (lowerCase.contains("!чирик")) {
            return createUserMessage(message, message.getUserName() + ", " + (Randomizer.tossCoin() ? "Вы чирикнулись {DOGGIE}" : "Вам не чирикнулось {DOGGIE}"));
        }
        ChatResponse result = answerWithoutCurrentUser(message, text, lowerCase);
        if (result == null) {
            result = answerForMaster(message, text, lowerCase);
        }
        return result;
    }

    private ChatResponse answerWithoutCurrentUser(ChatRequest message, String text, String lowerCase) {
        if (lowerCase.contains("догги")) {
            return createUserMessage(message, (Randomizer.getPercent() > 30 ? "{DOGGIE}" : "сам догги) {DOGGIE}"));
        }
        if (dailyStore.isTodayAllowed(message.getUserName() + "приветики") && lowerCase.contains("привет")) {
            return createUserMessage(message, "приветики {DOGGIE}");
        }
        if (lowerCase.contains("{DOGGIE}") && Randomizer.tossCoin()) {
            return createUserMessage(message, "{DOGGIE}");
        }
        return null;
    }

    private ChatResponse answerForMaster(ChatRequest message, String text, String lowerCase) {
        if (message.isForCurrentUser() && MASTER_NAME.equals(message.getUserName())) {
            if (lowerCase.contains("мой") || lowerCase.contains("моя")) {
                return createUserMessage(message, Randomizer.getRandomItem(MASTER_PERSONAL_MESSAGES));
            }
            return createUserMessage(message, Randomizer.getRandomItem(MASTER_MESSAGES));
        }
        return null;
    }

}
