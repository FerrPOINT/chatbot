package azhukov.chatbot.service.messages;

import azhukov.chatbot.dto.ReqGgMessage;
import azhukov.chatbot.dto.RespGgMessage;
import org.springframework.stereotype.Component;

@Component
public abstract class MessageHandler {

    public abstract ReqGgMessage answerMessage(RespGgMessage message, String text, String lowerCase);

    protected ReqGgMessage createUserMessage(RespGgMessage message, String text) {
        return new ReqGgMessage(message.getChannelId(), message.getUserName() + ", " + text, false, false);
    }

    protected ReqGgMessage createMessage(RespGgMessage message, String text) {
        return new ReqGgMessage(message.getChannelId(), text, false, false);
    }

    protected boolean isTanushka(RespGgMessage message) {
        return "TanushkaVL".equals(message.getUserName());
    }

}
