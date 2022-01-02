package azhukov.chatbot.service.messages;

import azhukov.chatbot.db.DbService;
import azhukov.chatbot.db.DbType;
import azhukov.chatbot.dto.ReqGgMessage;
import azhukov.chatbot.dto.RespGgMessage;
import azhukov.chatbot.service.Randomizer;
import azhukov.chatbot.service.pet.LifecycleService;
import azhukov.chatbot.service.pet.LifecycleStage;
import azhukov.chatbot.service.store.DailyStore;
import org.mapdb.DB;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LifeHandler extends MessageHandler {

    @Autowired
    private LifecycleService lifecycleService;

    @Override
    public ReqGgMessage answerMessage(RespGgMessage message, String text, String lowerCase) {
        return answerWithoutCurrentUser(message, text, lowerCase);
    }

    private ReqGgMessage answerWithoutCurrentUser(RespGgMessage message, String text, String lowerCase) {
        if (!message.isCurrentUser()) {
            if (lowerCase.contains("!кормить") || lowerCase.contains("!покормить")) {
                final LifecycleStage offset = lifecycleService.offset(+4);
                return createMessage(message, offset.getMessage() + " :doggie:");
            }
            if (lowerCase.contains("!отобрать") || lowerCase.contains("!отнять")) {
                final LifecycleStage offset = lifecycleService.offset(-1);
                return createMessage(message, offset.getMessage() + " :doggie:");
            }
            if (lowerCase.contains("!доген") || lowerCase.contains("!собаня")|| lowerCase.contains("!псинка")) {
                final LifecycleStage offset = lifecycleService.current();
                return createMessage(message, offset.getMessage() + " :doggie:");
            }
        }
        return null;
    }

}
