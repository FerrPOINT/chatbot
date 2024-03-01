package azhukov.chatbot.service.store;

import lombok.Value;

import java.util.Comparator;
import java.util.function.Function;

@Value
public class StoreUpdater<T> {
    Function<String, String> keyUpdater;
    Comparator<T> comparator;
}
