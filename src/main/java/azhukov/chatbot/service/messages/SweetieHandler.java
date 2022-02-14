package azhukov.chatbot.service.messages;

import azhukov.chatbot.dto.ReqGgMessage;
import azhukov.chatbot.dto.RespGgMessage;
import azhukov.chatbot.service.Randomizer;
import azhukov.chatbot.service.SweetieService;
import azhukov.chatbot.service.store.DailyStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SweetieHandler extends MessageHandler {

    @Autowired
    private SweetieService sweetieService;

    @Override
    public ReqGgMessage answerMessage(RespGgMessage message, String text, String lowerCase) {
        if (lowerCase.contains("!пупсик")) {
            String sweetie = sweetieService.getSweetie(message.getUserName());
            return createUserMessage(message, sweetie == null ? "У вас пока нет пупсика :doggie:" : ("Ваш пупсик: " + sweetie + " :doggie:"));
        }
        return null;
    }

}
