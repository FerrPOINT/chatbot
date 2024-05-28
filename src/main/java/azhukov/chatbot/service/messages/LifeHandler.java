package azhukov.chatbot.service.messages;

import azhukov.chatbot.dto.ChatRequest;
import azhukov.chatbot.dto.ChatResponse;
import azhukov.chatbot.service.pet.LifecycleService;
import azhukov.chatbot.service.pet.LifecycleStage;
import azhukov.chatbot.service.pet.LifecycleStore;
import azhukov.chatbot.service.users.UserBiteStore;
import azhukov.chatbot.service.util.Randomizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LifeHandler extends MessageHandler {

    private List<String> feed = List.of("!кормить", "!еда", "!накормить", "!покормить");
    @Autowired
    private LifecycleService lifecycleService;
    @Autowired
    private LifecycleStore lifecycleStore;
    @Autowired
    private UserBiteStore userBiteStore;

    private static final List<String> FEED_MESSAGES = List.of("Вы бросили кость собане", "У вас в кармане была сосиска и вы поделились с пёсиком", "Вы кормите псинку");
    private static final List<String> TAKE_MESSAGES = List.of("Собаня грызёт кость, но вы ловко её отнимаете", "У пёсика есть немного еды и вы отбираете её", "Вы крадёте еду пока собачка отвлеклась");


    private ChatResponse randomStray(ChatRequest message) {
        if (Randomizer.tossCoin()) {
            lifecycleService.reset();
            return createUserMessage(message, "У собачки итак много еды, но вы случайно привели за собой стаю бездомных догенов, которые всё отобрали у нашей пёси))) {DOGGIE}");
        }
        return null;
    }

    @Override
    public ChatResponse answerMessage(ChatRequest message, String text, String lowerCase) {
        if (feed.stream().anyMatch(lowerCase::contains)) {
            if (lifecycleStore.isAllowedToFeed(message)) {
                LifecycleStage current = lifecycleService.current();
                if (current.isMax()) {
                    ChatResponse stray = randomStray(message);
                    if (stray != null) {
                        return stray;
                    }
                    return createUserMessage(message, "Пёсонька уже итак перекормлен {DOGGIE}");
                }
                final LifecycleStage offset = lifecycleService.offset(+2);
                return createUserMessage(message, Randomizer.getRandomItem(FEED_MESSAGES) + ". " + offset.getMessage() + " {DOGGIE}");
            }
            ChatResponse stray = randomStray(message);
            if (stray != null) {
                return stray;
            }
            return createUserMessage(message, "Сегодня вы уже покормили пёсика и он вам благодарен {DOGGIE}");
        }
        if (lowerCase.contains("!отобрать") || lowerCase.contains("!отнять")) {
            if (lifecycleStore.isAllowedToTake(message)) {
                LifecycleStage current = lifecycleService.current();
                if (current.isMin()) {
                    return createUserMessage(message, "У собачки итак ничего нет - отнимать нечего {DOGGIE}");
                }
                final LifecycleStage offset = lifecycleService.offset(-1);
                return createUserMessage(message, Randomizer.getRandomItem(TAKE_MESSAGES) + ". " + offset.getMessage() + " {DOGGIE}");
            }

            int biteCount = userBiteStore.bite(message.getUserName());
            return createUserMessage(message, "Собаня запомнил того, кто забирал у него еду и он делает вам кусь {DOGGIE}" + (biteCount > 1 ? (" Вы уже покусаны " + biteCount + " раз") : ""));
        }
        if (lowerCase.contains("!доген") || lowerCase.contains("!собаня") || lowerCase.contains("!псинка")) {
            final LifecycleStage offset = lifecycleService.current();
            return createMessage(message, offset.getMessage() + " {DOGGIE}");
        }
        return null;
    }

}
