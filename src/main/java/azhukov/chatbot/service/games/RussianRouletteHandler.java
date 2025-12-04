package azhukov.chatbot.service.games;

import azhukov.chatbot.dto.ChatRequest;
import azhukov.chatbot.dto.ChatResponse;
import azhukov.chatbot.service.messages.MessageHandler;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class RussianRouletteHandler extends MessageHandler {

    private static final String COMMAND = "!рулетка";

    @Override
    public ChatResponse answerMessage(ChatRequest message, String text, String lowerCase) {
        if (lowerCase.startsWith(COMMAND)) {
            boolean survived = ThreadLocalRandom.current().nextInt(6) != 0;
            if (survived) {
                return createUserMessage(message, "выжил! :)");
            } else {
                return createUserMessage(message, "погиб! :(");
            }
        }
        return null;
    }
}
