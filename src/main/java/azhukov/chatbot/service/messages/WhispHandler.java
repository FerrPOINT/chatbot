package azhukov.chatbot.service.messages;

import azhukov.chatbot.dto.ChatRequest;
import azhukov.chatbot.dto.ChatResponse;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class WhispHandler extends MessageHandler {

    private static final String[] SAY_PARTS = {"!сказать", "!скажи"};

    @Override
    public ChatResponse answerMessage(ChatRequest message, String text, String lowerCase) {
        if (message.getUserName() == null && Arrays.stream(SAY_PARTS).anyMatch(lowerCase::contains)) {
            return createMessage(message, message.getText().substring(message.getText().indexOf(" ") + 1));
        }
        return null;
    }
}
