package azhukov.chatbot.service.messages;

import azhukov.chatbot.dto.ReqGgMessage;
import azhukov.chatbot.dto.RespGgMessage;
import azhukov.chatbot.service.dictionary.DictionaryService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class DictionaryHandler extends MessageHandler {

    private final DictionaryService dictionaryService;

    @Override
    public ReqGgMessage answerMessage(RespGgMessage message, String text, String lowerCase) {
        final String dictionaryAnswer = dictionaryService.getDictionaryAnswer(message.getUserName(), lowerCase);
        if (dictionaryAnswer != null) {
            return createUserMessage(message, dictionaryAnswer);
        }
        return null;
    }
}
