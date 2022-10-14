package azhukov.chatbot.service.messages;

import azhukov.chatbot.dto.ReqGgMessage;
import azhukov.chatbot.dto.RespGgMessage;
import azhukov.chatbot.service.Randomizer;
import azhukov.chatbot.service.fight.Fight;
import azhukov.chatbot.service.fight.FightService;
import azhukov.chatbot.service.store.DailyStore;
import azhukov.chatbot.service.store.Store;
import azhukov.chatbot.service.users.UserCollectionStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class FightHandler extends MessageHandler {
    private static final String STORE_KEY = "FIGHT_STORE_KEY";
    private final FightService fightService;
    private final UserCollectionStore userCollectionStore;
    private final DailyStore dailyStore;

    private static final List<String> COMMANDS = List.of("!собачьи бои", "!бой", "!арена", "!битва");

    @PostConstruct
    void init() {
        dailyStore.getStore(STORE_KEY);
    }

    @Override
    public ReqGgMessage answerMessage(RespGgMessage message, String text, String lowerCase) {
        for (String command : COMMANDS) {
            if (lowerCase.startsWith(command)) {

                int currentDailyTries = getCurrentDailyTries(message.getUserName());

                if (currentDailyTries > 5) {
                    return createUserMessage(message, "Ты сегодня уже нааренился :doggie:");
                }

                Fight fight = fightService.fight(message.getUserName());
                if (fight == null) {
                    return createUserMessage(message, "Вы уже участвуете :doggie:");
                }
                if (fight.getSecondUser() == null) {
                    return createUserMessage(message, "Ожидайте второго :doggie:");
                }
                swapRandomly(fight);
                int percent = Randomizer.getPercent();
                Set<String> firstCollection = getSet(fight.getFirstUser());
                Set<String> secondCollection = getSet(fight.getSecondUser());

                String firstItem = CollectionUtils.isEmpty(firstCollection) ? null : Randomizer.getRandomItem(new ArrayList<>(firstCollection));
                String secondItem = CollectionUtils.isEmpty(secondCollection) ? null : Randomizer.getRandomItem(new ArrayList<>(secondCollection));

                String prefix = "Битва! " + fight.getFirstUser() + (firstItem == null ? " рискует жопкой" : " талисман " + firstItem) + " VS " + fight.getSecondUser() + (secondItem == null ? " рискует жопкой" : " талисман " + secondItem) + ". ";
                String resultMessage;
                if (percent < 45) {
                    resultMessage = createWinMessage(fight.getFirstUser(), fight.getSecondUser(), firstCollection, secondCollection, secondItem);
                } else if (percent > 55) {
                    resultMessage = createWinMessage(fight.getSecondUser(), fight.getFirstUser(), secondCollection, firstCollection, firstItem);
                } else {
                    resultMessage = "Долго бились, но это ничья. Оба нубаськи остаются при своём";
                }
                return createMessage(message, prefix + resultMessage);
            }
        }
        return null;
    }

    private int getCurrentDailyTries(String user) {
        Store store = dailyStore.getStore(STORE_KEY);
        String now = store.get(user);
        if (now == null) {
            store.put(user, "1");
            return 1;
        }
        int nowInt = Integer.parseInt(now);
        store.put(user, String.valueOf(nowInt++));
        return nowInt;
    }

    private int incrementAndGetCurrentDailyTries(String user) {
        Store store = dailyStore.getStore(STORE_KEY);
        String now = store.get(user);
        return now == null ? 0 : Integer.parseInt(now);
    }

    private void swapRandomly(Fight fight) {
        if (Randomizer.tossCoin()) {
            String firstUser = fight.getFirstUser();
            String secondUser = fight.getSecondUser();
            fight.setFirstUser(secondUser);
            fight.setSecondUser(firstUser);
        }
    }

    private Set<String> getSet(String userName) {
        Set<String> talisman = userCollectionStore.getCurrentSet(userName, "talisman");
        return talisman == null ? new HashSet<>() : talisman;
    }

    private String createWinMessage(String winUser, String secondUser, Set<String> winCollection, Set<String> secondCollection, String lostItem) {
        if (lostItem == null) {
            return "Победитель " + winUser + " забирает жопку противника так как у него не оказалось талисманов";
        } else {
            winCollection.add(lostItem);
            secondCollection.remove(lostItem);
            userCollectionStore.save(winUser, winCollection, "talisman");
            userCollectionStore.save(secondUser, secondCollection, "talisman");
        }
        return "Победитель " + winUser + " забирает талиман противника: " + lostItem;
    }

}
