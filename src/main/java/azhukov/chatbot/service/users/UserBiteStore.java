package azhukov.chatbot.service.users;

import azhukov.chatbot.service.store.DailyStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserBiteStore {

    private final DailyStore dailyStore;

    public int bite(String user) {
        return dailyStore.incrementAndGet(createKey(user));
    }

    private String createKey(String user) {
      return "USER_BITE_" + user.toUpperCase();
    }

}
