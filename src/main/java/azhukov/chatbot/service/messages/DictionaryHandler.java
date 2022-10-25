package azhukov.chatbot.service.messages;

import azhukov.chatbot.dto.ChatRequest;
import azhukov.chatbot.dto.ChatResponse;
import azhukov.chatbot.service.dictionary.DictionaryService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@AllArgsConstructor
public class DictionaryHandler extends MessageHandler {

    private final DictionaryService dictionaryService;

    @Override
    public ChatResponse answerMessage(ChatRequest message, String text, String lowerCase) {
        if (lowerCase.contains("!талисманы")) {
            List<String> talismansList = dictionaryService.getTalismansList(message.getUserName());
            return createUserMessage(message, talismansList.isEmpty() ? "У вас нет талисманов" : ("Ваши талисманы: " + String.join(", ", talismansList)));
        }
        final String dictionaryAnswer = dictionaryService.getDictionaryAnswer(message.getUserName(), lowerCase);
        if (dictionaryAnswer != null) {
            return createUserMessage(message, dictionaryAnswer);
        }
        return null;
    }
}
