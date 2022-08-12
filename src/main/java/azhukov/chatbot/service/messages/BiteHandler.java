package azhukov.chatbot.service.messages;

import azhukov.chatbot.dto.ReqGgMessage;
import azhukov.chatbot.dto.RespGgMessage;
import azhukov.chatbot.service.Randomizer;
import azhukov.chatbot.service.users.UserBiteStore;
import azhukov.chatbot.service.users.UserMessageStore;
import azhukov.chatbot.service.util.CommandsUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BiteHandler extends MessageHandler {

    private final UserMessageStore dailyUsersStore;
    private final UserBiteStore userBiteStore;

    private static final List<String> BITE_COMMANDS = List.of("!укусить", "!кусить", "!фас");
    // TODO macros
    private static final  List<String> BITE_MESSAGES = List.of("Вы были укушены псинкой", "Вы были покусаны собачкой из чата", "Вам делают лёгкий кусь");

    @Override
    public ReqGgMessage answerMessage(RespGgMessage message, String text, String lowerCase) {
        for (String biteCommand : BITE_COMMANDS) {
            String nextWordAfterCommand = CommandsUtil.getNextWordAfterCommand(text, lowerCase, biteCommand);
            if (nextWordAfterCommand != null) {
                int todayMessagesCount = dailyUsersStore.getTodayMessagesCount(nextWordAfterCommand);
                if (todayMessagesCount > 0) {
                    int biteCount = userBiteStore.bite(nextWordAfterCommand);
                    return createUserMessage(message, Randomizer.getRandomItem(BITE_MESSAGES) + " :doggie:" + (biteCount > 1 ? (" Вы были укушены " + biteCount + " раз") : ""), nextWordAfterCommand);
                } else {
                    return createUserMessage(message, "Тут таких не обитает :doggie:");
                }
            }
        }
        return null;
    }
}
