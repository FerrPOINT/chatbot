package azhukov.chatbot.service.weight;

import azhukov.chatbot.util.Range;
import azhukov.chatbot.util.RangesContainer;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.List;
import java.util.stream.Collectors;

@UtilityClass
public final class WeightUtils {

    public static <T extends WeightItem> RangesContainer<T> createContainer(List<T> items) {
        MutableInt prevEnd = new MutableInt();
        return new RangesContainer<>(items.stream().map(t -> new Range<>(prevEnd.incrementAndGet(), prevEnd.addAndGet(t.getWeight().getValue() - 1), t)).collect(Collectors.toList()));
    }

}
