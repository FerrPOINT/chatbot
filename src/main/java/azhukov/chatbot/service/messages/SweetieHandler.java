package azhukov.chatbot.service.messages;

import azhukov.chatbot.constants.Constants;
import azhukov.chatbot.dto.ChatRequest;
import azhukov.chatbot.dto.ChatResponse;
import azhukov.chatbot.service.SweetieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SweetieHandler extends MessageHandler {

    @Autowired
    private SweetieService sweetieService;

    @Override
    public ChatResponse answerMessage(ChatRequest message, String text, String lowerCase) {
        if (lowerCase.contains("!пупсик")) {
            if (Constants.MASTER_NAME.equals(message.getUserName())) {
                return createUserMessage(message, "Весь чат ваш пупсик {DOGGIE}");
            }
            String sweetie = sweetieService.getSweetie(message.getUserName());
            return createUserMessage(message, sweetie == null ? "У вас пока нет пупсика {DOGGIE}" : ("Ваш пупсик: " + sweetie + " {DOGGIE}"));
        }
        if (lowerCase.contains("!непупсик")) {
            sweetieService.deleteSweetie(message.getUserName());
            return createUserMessage(message, "Ах ты не пупсик {DOGGIE}");
        }
        return null;
    }

}
