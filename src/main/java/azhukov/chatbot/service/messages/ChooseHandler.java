package azhukov.chatbot.service.messages;

import azhukov.chatbot.dto.ReqGgMessage;
import azhukov.chatbot.dto.RespGgMessage;
import azhukov.chatbot.service.Randomizer;
import org.springframework.stereotype.Component;

@Component
public class ChooseHandler extends MessageHandler {

    @Override
    public ReqGgMessage answerMessage(RespGgMessage message, String text, String lowerCase) {
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
