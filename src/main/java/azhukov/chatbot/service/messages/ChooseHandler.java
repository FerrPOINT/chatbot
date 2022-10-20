package azhukov.chatbot.service.messages;

import azhukov.chatbot.dto.ChatRequest;
import azhukov.chatbot.dto.ChatResponse;
import azhukov.chatbot.service.util.Randomizer;
import org.springframework.stereotype.Component;

@Component
public class ChooseHandler extends MessageHandler {

    @Override
    public ChatResponse answerMessage(ChatRequest message, String text, String lowerCase) {
        if (lowerCase.contains("!выбор")) {
            String[] tokenized = lowerCase.split(" ");
            if (tokenized.length == 4 || tokenized.length == 3) {
                return createUserMessage(message, Randomizer.tossCoin() ? tokenized[1] : tokenized[tokenized.length == 3 ? 2 : 3]);
            }
            if (lowerCase.contains("или")) {
                tokenized = lowerCase.substring(lowerCase.indexOf(" ") + 1).split("или");
                return createUserMessage(message, Randomizer.tossCoin() ? tokenized[0].trim() : tokenized[1].trim());
            }
        }
        return null;
    }
}
