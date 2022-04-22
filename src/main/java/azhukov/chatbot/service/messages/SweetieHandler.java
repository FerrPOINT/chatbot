package azhukov.chatbot.service.messages;

import azhukov.chatbot.constants.Constants;
import azhukov.chatbot.dto.ReqGgMessage;
import azhukov.chatbot.dto.RespGgMessage;
import azhukov.chatbot.service.SweetieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SweetieHandler extends MessageHandler {

    @Autowired
    private SweetieService sweetieService;

    @Override
    public ReqGgMessage answerMessage(RespGgMessage message, String text, String lowerCase) {
        if (lowerCase.contains("!пупсик")) {
            if (Constants.MASTER_NAME.equals(message.getUserName())) {
                return createUserMessage(message, "Весь чат ваш пупсик :doggie:");
            }
            String sweetie = sweetieService.getSweetie(message.getUserName());
            return createUserMessage(message, sweetie == null ? "У вас пока нет пупсика :doggie:" : ("Ваш пупсик: " + sweetie + " :doggie:"));
        }
        if (lowerCase.contains("!непупсик")) {
            sweetieService.deleteSweetie(message.getUserName());
            return createUserMessage(message, "Ах ты не пупсик :doggie:");
        }
        return null;
    }

}
