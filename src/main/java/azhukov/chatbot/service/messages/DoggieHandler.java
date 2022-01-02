package azhukov.chatbot.service.messages;

import azhukov.chatbot.dto.ReqGgMessage;
import azhukov.chatbot.dto.RespGgMessage;
import azhukov.chatbot.service.Randomizer;
import azhukov.chatbot.service.store.DailyStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DoggieHandler extends MessageHandler {

    private static String MASTER_NAME = "TanushkaVL";
    private static List<String> MASTER_MESSAGES = List.of("Аффьььь :doggie:", "ррряф :doggie:", "Вуфь? :doggie:", "тяфф из-за угла :doggie:", "аф-кусь :doggie:", "ну допустим вуф :doggie:");
    private static List<String> MASTER_PERSONAL_MESSAGES = List.of("авууу :doggie: :love:", "!любовь :doggie:", "афь афь :love: (ластится)", "вуфь, кусь за зёпку :doggie:");

    @Autowired
    private DailyStore dailyStore;

    @Override
    public ReqGgMessage answerMessage(RespGgMessage message, String text, String lowerCase) {
        if (lowerCase.contains("догги")) {
            return createUserMessage(message, (Randomizer.getPercent() > 30 ? ":doggie:" : "сам догги) :doggie:"));
        }
        if (lowerCase.contains("!ауф")) {
            return createUserMessage(message, "Все мои догги делают ауф :doggie:");
        }
        if (lowerCase.contains("!трусики")) {
            return new ReqGgMessage(message.getChannelId(), message.getUserName() + ", " + (Randomizer.tossCoin() ? "Вы посмотрели на трусики :tanushkavl29:" : "Вам не удалось посмотреть на трусики :tanushkavl19:"), false, false);
        }
        if (lowerCase.contains("!чирик")) {
            return new ReqGgMessage(message.getChannelId(), message.getUserName() + ", " + (Randomizer.tossCoin() ? "Вы чирикнулись :doggie:" : "Вам не чирикнулось :doggie:"), false, false);
        }
        ReqGgMessage result = answerWithoutCurrentUser(message, text, lowerCase);
        if (result == null) {
            result = answerForMaster(message, text, lowerCase);
        }
        return result;
    }

    private ReqGgMessage answerWithoutCurrentUser(RespGgMessage message, String text, String lowerCase) {
        if (!message.isCurrentUser()) {
            if (dailyStore.isTodayAllowed(message.getUserName() + "приветики") && lowerCase.contains("привет")) {
                return createUserMessage(message, "приветики :doggie:");
            }
            if (lowerCase.contains(":doggie:") && Randomizer.tossCoin()) {
                return createUserMessage(message, ":doggie:");
            }
        }
        return null;
    }

    private ReqGgMessage answerForMaster(RespGgMessage message, String text, String lowerCase) {
        if (message.isForCurrentUser() && MASTER_NAME.equals(message.getUserName())) {
            if (lowerCase.contains("мой") || lowerCase.contains("моя")) {
                return createUserMessage(message, Randomizer.getRandomItem(MASTER_PERSONAL_MESSAGES));
            }
            return createUserMessage(message, Randomizer.getRandomItem(MASTER_MESSAGES));
        }
        return null;
    }

}
