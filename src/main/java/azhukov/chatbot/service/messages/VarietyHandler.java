package azhukov.chatbot.service.messages;

import azhukov.chatbot.dto.ReqGgMessage;
import azhukov.chatbot.dto.RespGgMessage;
import azhukov.chatbot.service.variety.VarietiesService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class VarietyHandler extends MessageHandler {

    private final VarietiesService varietiesService;

    @Override
    public ReqGgMessage answerMessage(RespGgMessage message, String text, String lowerCase) {
        String varietyMessage = varietiesService.getVarietyMessage(message.getUserName(), lowerCase);
        return varietyMessage == null ? null : createUserMessage(message, varietyMessage + " :doggie:");
    }
}
