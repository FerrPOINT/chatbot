package azhukov.chatbot.service.messages;

import azhukov.chatbot.dto.ReqGgMessage;
import azhukov.chatbot.dto.RespGgMessage;
import azhukov.chatbot.service.dictionary.DictionaryService;
import azhukov.chatbot.service.pet.LifecycleService;
import azhukov.chatbot.service.pet.LifecycleStage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TalismanHandler extends MessageHandler {

    @Autowired
    private DictionaryService dictionaryService;

    @Override
    public ReqGgMessage answerMessage(RespGgMessage message, String text, String lowerCase) {
        return answerWithoutCurrentUser(message, text, lowerCase);
    }

    private ReqGgMessage answerWithoutCurrentUser(RespGgMessage message, String text, String lowerCase) {
//        if (!message.isCurrentUser()) {
//            dictionaryService.getDictionaryAnswer()
//
//
//            if (lowerCase.contains("!кормить")) {
//                final LifecycleStage offset = lifecycleService.offset(+1);
//                return createMessage(message, offset.getMessage() + " :doggie:");
//            }
//            if (lowerCase.contains("!отобрать")) {
//                final LifecycleStage offset = lifecycleService.offset(-1);
//                return createMessage(message, offset.getMessage() + " :doggie:");
//            }
//            if (lowerCase.contains("!доген") || lowerCase.contains("!собаня")) {
//                final LifecycleStage offset = lifecycleService.current();
//                return createMessage(message, offset.getMessage() + " :doggie:");
//            }
//        }
        return null;
    }

}
