package azhukov.chatbot.service.messages;

import azhukov.chatbot.dto.ChatRequest;
import azhukov.chatbot.dto.ChatResponse;
import azhukov.chatbot.service.dictionary.DictionaryService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class DictionaryHandler extends MessageHandler {

    private final DictionaryService dictionaryService;

    @Override
    public ChatResponse answerMessage(ChatRequest message, String text, String lowerCase) {
        final String dictionaryAnswer = dictionaryService.getDictionaryAnswer(message.getUserName(), lowerCase);
        if (dictionaryAnswer != null) {
            return createUserMessage(message, dictionaryAnswer);
        }
        return null;
    }
}
