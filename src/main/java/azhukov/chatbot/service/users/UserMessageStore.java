package azhukov.chatbot.service.users;

import azhukov.chatbot.service.store.DailyStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserMessageStore {

    private final DailyStore dailyStore;

    public int getTodayMessagesCount(String user) {
        return dailyStore.getCount(createKey(user));
    }

    public int countMessage(String user) {
        return dailyStore.incrementAndGet(createKey(user));
    }

    private String createKey(String user) {
      return "USER_MESSAGE_" + user.toUpperCase();
    }

}
