package azhukov.chatbot.service.messages;

import azhukov.chatbot.dto.ChatRequest;
import azhukov.chatbot.dto.ChatResponse;
import azhukov.chatbot.service.combination.CombinationService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class CombinationHandler extends MessageHandler {

    private final CombinationService combinationService;

    @Override
    public ChatResponse answerMessage(ChatRequest message, String text, String lowerCase) {
        String combinationMessage = combinationService.getCombinationMessage(message.getUserName(), lowerCase);
        return combinationMessage == null ? null : createUserMessage(message, combinationMessage);
    }

}