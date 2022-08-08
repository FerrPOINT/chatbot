package azhukov.chatbot.service.messages;

import azhukov.chatbot.dto.ReqGgMessage;
import azhukov.chatbot.dto.RespGgMessage;
import azhukov.chatbot.service.Randomizer;
import azhukov.chatbot.service.pet.LifecycleService;
import azhukov.chatbot.service.pet.LifecycleStage;
import azhukov.chatbot.service.pet.LifecycleStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class LifeHandler extends MessageHandler {

    private List<String> feed = List.of("!кормить","!еда","!накормить","!покормить");
    @Autowired
    private LifecycleService lifecycleService;
    @Autowired
    private LifecycleStore lifecycleStore;

    private static final List<String> FEED_MESSAGES = List.of("Вы бросили кость собане", "У вас в кармане была сосиска и вы поделились с пёсиком", "Вы кормите псинку");
    private static final List<String> TAKE_MESSAGES = List.of("Собаня грызёт кость, но вы ловко её отнимаете", "У пёсика есть немного еды и вы отбираете её", "Вы крадёте еду пока собачка отвлеклась");

    @Override
    public ReqGgMessage answerMessage(RespGgMessage message, String text, String lowerCase) {
        return answerWithoutCurrentUser(message, text, lowerCase);
    }

    private ReqGgMessage answerWithoutCurrentUser(RespGgMessage message, String text, String lowerCase) {
        if (!message.isCurrentUser()) {
            if (feed.stream().anyMatch(lowerCase::contains) ) {
                if (lifecycleStore.isAllowedToFeed(message)) {
                    LifecycleStage current = lifecycleService.current();
                    if (current.isMax()) {
                        return createUserMessage(message, "Пёсонька уже итак перекормлен :doggie:");
                    }
                    final LifecycleStage offset = lifecycleService.offset(+2);
                    return createUserMessage(message, Randomizer.getRandomItem(FEED_MESSAGES) + ". " + offset.getMessage() + " :doggie:");
                }
                return createUserMessage(message, "Сегодня вы уже покормили пёсика и он вам благодарен :doggie:");
            }
            if (lowerCase.contains("!отобрать") || lowerCase.contains("!отнять")) {
                if (lifecycleStore.isAllowedToTake(message)) {
                    LifecycleStage current = lifecycleService.current();
                    if (current.isMin()) {
                        return createUserMessage(message, "У собачки итак ничего нет - отнимать нечего :doggie:");
                    }
                    final LifecycleStage offset = lifecycleService.offset(-1);
                    return createUserMessage(message, Randomizer.getRandomItem(TAKE_MESSAGES) + ". " + offset.getMessage() + " :doggie:");
                }
                return createUserMessage(message, "Собаня запомнил того, кто забирал у него еду и он делает вам кусь :doggie:");
            }
            if (lowerCase.contains("!доген") || lowerCase.contains("!собаня")|| lowerCase.contains("!псинка")) {
                final LifecycleStage offset = lifecycleService.current();
                return createMessage(message, offset.getMessage() + " :doggie:");
            }
        }
        return null;
    }

}
