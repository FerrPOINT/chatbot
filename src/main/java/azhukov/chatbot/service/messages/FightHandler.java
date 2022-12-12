package azhukov.chatbot.service.messages;

import azhukov.chatbot.dto.ChatRequest;
import azhukov.chatbot.dto.ChatResponse;
import azhukov.chatbot.service.arena.Fight;
import azhukov.chatbot.service.arena.FightService;
import azhukov.chatbot.service.store.DailyStore;
import azhukov.chatbot.service.store.Store;
import azhukov.chatbot.service.users.UserCollectionStore;
import azhukov.chatbot.service.util.Randomizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.*;

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
    public ChatResponse answerMessage(ChatRequest message, String text, String lowerCase) {
        for (String command : COMMANDS) {
            if (lowerCase.startsWith(command)) {
                Set<String> set = getSet(message.getUserName());
                if (set.isEmpty()) {
                    return createUserMessage(message, "У вас нет талисманов для арены {DOGGIE}");
                }
                Fight fight = fightService.fight(message.getUserName());
                if (fight == null) {
                    return createUserMessage(message, "Вы уже участвуете {DOGGIE}");
                }
//                int currentDailyTries = getCurrentDailyTries(message.getUserName());
//                if (currentDailyTries > 5) {
//                    fight.setFirstUser(null);
//                    return createUserMessage(message, "Ты сегодня уже нааренился {DOGGIE}");
//                }
                if (fight.getSecondUser() == null) {
                    return createUserMessage(message, "Ожидайте второго {DOGGIE}");
                }
//                swapRandomly(fight);
                Set<String> firstCollection = getSet(fight.getFirstUser());
                Set<String> secondCollection = getSet(fight.getSecondUser());

                UserSets unique = findUnique(firstCollection, secondCollection);

                String firstItem;
                String secondItem;
                if (unique.isEmpty()) {
                    String randomIntersectItem = getRandomIntersectItem(firstCollection, secondCollection);
                    firstItem = randomIntersectItem;
                    secondItem = randomIntersectItem;
                } else {
                    firstItem = CollectionUtils.isEmpty(unique.firstCollection) ? null : Randomizer.getRandomItem(new ArrayList<>(unique.firstCollection));
                    secondItem = CollectionUtils.isEmpty(unique.secondCollection) ? null : Randomizer.getRandomItem(new ArrayList<>(unique.secondCollection));
                }

                String prefix = "Битва! " + fight.getFirstUser() + (firstItem == null ? " рискует жопкой" : " талисман " + firstItem) + " VS " + fight.getSecondUser() + (secondItem == null ? " рискует жопкой" : " талисман " + secondItem) + ". ";
                String resultMessage;

                if (Objects.equals(firstItem, secondItem)) {
                    if (firstCollection.size() == secondCollection.size()) {
                        resultMessage = "Долго бились, но это ничья. Оба нубаськи остаются при своём";
                    } else {
                        String bigger = firstCollection.size() > secondCollection.size() ? fight.getFirstUser() : fight.getSecondUser();
                        String lower = firstCollection.size() > secondCollection.size() ? fight.getSecondUser() : fight.getFirstUser();
                        resultMessage = bigger + " не заинтересован в талисманах " + lower + " поищите других соперников";
                        prefix = "";
                    }
                } else {
                    int percent = Randomizer.getPercent();
                    if (percent % 10 <= 4) {
                        resultMessage = createWinMessage(fight.getFirstUser(), secondItem);
                        saveData(fight.getFirstUser(), fight.getSecondUser(), firstCollection, secondCollection, secondItem);
                    } else {
                        resultMessage = createWinMessage(fight.getSecondUser(), firstItem);
                        saveData(fight.getSecondUser(), fight.getFirstUser(), secondCollection, firstCollection, firstItem);
                    }
                }
                return createMessage(message, prefix + resultMessage);
            }
        }
        return null;
    }

    private UserSets findUnique(Set<String> firstCollection, Set<String> secondCollection) {
        UserSets userSets = new UserSets();
        userSets.firstCollection = new HashSet<>(firstCollection);
        userSets.secondCollection = new HashSet<>(secondCollection);
        userSets.firstCollection.removeAll(secondCollection);
        userSets.secondCollection.removeAll(firstCollection);
        return userSets;
    }

    private String getRandomIntersectItem(Set<String> firstCollection, Set<String> secondCollection) {
        HashSet<String> newFirst = new HashSet<>(firstCollection);
        newFirst.retainAll(secondCollection);
        if (newFirst.isEmpty()) {
            return null;
        }
        return Randomizer.getRandomItem(new ArrayList<>(newFirst));
    }

    private static class UserSets {
        Set<String> firstCollection;
        Set<String> secondCollection;

        private boolean isEmpty() {
            return CollectionUtils.isEmpty(firstCollection) || CollectionUtils.isEmpty(secondCollection);
        }
    }

    private int getCurrentDailyTries(String user) {
        Store store = dailyStore.getStore(STORE_KEY);
        String now = store.get(user);
        int nowInt = now == null ? 0 : Integer.parseInt(now);
        nowInt++;
        store.put(user, String.valueOf(nowInt));
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

    private String createWinMessage(String winUser, String lostItem) {
        if (lostItem == null) {
            return "Победитель " + winUser + " забирает жопку противника так как у него не оказалось талисманов";
        } else {
            return "Победитель " + winUser + " забирает талиман противника: " + lostItem;
        }
    }

    private void saveData(String winUser, String secondUser, Set<String> winCollection, Set<String> secondCollection, String lostItem) {
        winCollection.add(lostItem);
        secondCollection.remove(lostItem);
        userCollectionStore.save(winUser, winCollection, "talisman");
        userCollectionStore.save(secondUser, secondCollection, "talisman");
    }

}
