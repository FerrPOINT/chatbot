package azhukov.chatbot.service.messages;

import azhukov.chatbot.dto.ChatRequest;
import azhukov.chatbot.dto.ChatResponse;
import azhukov.chatbot.service.variety.VarietiesService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class VarietyHandler extends MessageHandler {

    private final VarietiesService varietiesService;

    @Override
    public ChatResponse answerMessage(ChatRequest message, String text, String lowerCase) {
        String varietyMessage = varietiesService.getVarietyMessage(message.getUserName(), lowerCase);
        return varietyMessage == null ? null : createUserMessage(message, varietyMessage + " {DOGGIE}");
    }
}
