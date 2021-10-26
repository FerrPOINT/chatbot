package azhukov.chatbot.service.messages;

import azhukov.chatbot.dto.ReqGgMessage;
import azhukov.chatbot.dto.RespGgMessage;
import azhukov.chatbot.service.TalismanChooser;
import org.springframework.stereotype.Component;

@Component
public class TalismanHandler extends MessageHandler {

    @Override
    public ReqGgMessage answerMessage(RespGgMessage message, String text, String lowerCase) {
        if (lowerCase.contains("!список талисманов")) {
            return createUserMessage(message, TalismanChooser.getTalismansList());
        }
        if (lowerCase.contains("!талисман")) {
            return createUserMessage(message, TalismanChooser.getTalismanMessage(message.getUserName()));
        }
        return null;
    }
}
