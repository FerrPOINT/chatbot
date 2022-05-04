package azhukov.chatbot.service.pet;

import azhukov.chatbot.dto.RespGgMessage;
import azhukov.chatbot.service.store.DailyStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LifecycleStore {

    private final DailyStore dailyStore;

    public boolean isAllowedToTake(RespGgMessage message) {
        return isAllowed(message.getUserName(), FoodType.TAKE);
    }

    public boolean isAllowedToFeed(RespGgMessage message) {
        return isAllowed(message.getUserName(), FoodType.FEED);
    }

    private boolean isAllowed(String user, FoodType foodType) {
        return dailyStore.isTodayAllowed(user + foodType);
    }

    private enum FoodType {
        FEED,
        TAKE,
    }

}
