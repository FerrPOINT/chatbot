package azhukov.chatbot.service.messages;

import azhukov.chatbot.dto.ChatRequest;
import azhukov.chatbot.dto.ChatResponse;
import azhukov.chatbot.service.users.UserBiteStore;
import azhukov.chatbot.service.users.UserStore;
import azhukov.chatbot.service.util.CommandsUtil;
import azhukov.chatbot.service.util.Randomizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BiteHandler extends MessageHandler {

    private final UserStore dailyUsersStore;
    private final UserBiteStore userBiteStore;

    private static final List<String> BITE_COMMANDS = List.of("!укусить", "!кусить", "!фас");
    // TODO macros
    private static final List<String> BITE_MESSAGES = List.of(
            "Вы были укушены псинкой",
            "Вы были покусаны собачкой из чата",
            "Вам делают лёгкий кусь",
            "Ваc отаковал собаня, получите кусь"
    );

    @Override
    public ChatResponse answerMessage(ChatRequest message, String text, String lowerCase) {
        for (String biteCommand : BITE_COMMANDS) {
            String targetName = CommandsUtil.getNextWordAfterCommand(text, lowerCase, biteCommand);
            if (targetName != null) {
                if (dailyUsersStore.isExist(targetName)) {
                    int biteCount = userBiteStore.bite(targetName);
                    return createUserMessage(message, Randomizer.getRandomItem(BITE_MESSAGES) + " {DOGGIE}" + (biteCount > 1 ? (" Вы были укушены " + biteCount + " раз") : ""), targetName);
                } else {
                    return createUserMessage(message, "Тут таких не обитает {DOGGIE}");
                }
            }
        }
        return null;
    }
}
