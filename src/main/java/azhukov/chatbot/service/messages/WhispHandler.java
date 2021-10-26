package azhukov.chatbot.service.messages;

import azhukov.chatbot.dto.ReqGgMessage;
import azhukov.chatbot.dto.RespGgMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class WhispHandler extends MessageHandler {

    private static final String[] SAY_PARTS = {"!сказать", "!скажи"};

    @Value("${checked-channels}")
    private int channelId;

    @Override
    public ReqGgMessage answerMessage(RespGgMessage message, String text, String lowerCase) {
        if (message.getUserName() == null && Arrays.stream(SAY_PARTS).anyMatch(lowerCase::contains)) {
            return new ReqGgMessage(channelId, message.getText().substring(message.getText().indexOf(" ") + 1), false, false);
        }
        return null;
    }
}
