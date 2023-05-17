package azhukov.chatbot.util;

import azhukov.chatbot.service.util.Randomizer;
import lombok.Data;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Data
public class RangesContainer<T> {

    private final List<Range<T>> ranges;
    @Getter
    private final int lastPoint;
    @Getter
    private final int firstPoint;

    public RangesContainer(List<Range<T>> ranges) {
        List<Range<T>> newList = new ArrayList<>(ranges);
        newList.sort(Comparator.comparingInt(Range::start));
        this.ranges = newList;
        firstPoint = ranges.stream().min(Comparator.comparingInt(Range::start)).get().start();
        lastPoint = ranges.stream().max(Comparator.comparingInt(Range::end)).get().end();
    }

    public T getRandomItem() {
        return getItem(Randomizer.nextInt(firstPoint, lastPoint + 1));
    }

    public T getItem(int number) {
        if (number < firstPoint || number > lastPoint) {
            return null;
        }
        return getItem(number, 0, ranges.size() - 1);
    }

    private T getItem(int number, int startIndex, int endIndex) {
        if (startIndex == endIndex) {
            Range<T> range = ranges.get(startIndex);
            if (in(number, range)) {
                return range.item();
            }
        }
        int center = startIndex + (endIndex - startIndex) / 2;
        Range<T> centerRange = ranges.get(center);
        if (in(number, centerRange)) {
            return centerRange.item();
        }

        if (number < centerRange.start()) {
            return getItem(number, startIndex, center);
        }

        if (number > centerRange.end()) {
            if (center == startIndex) {
                center++;
            }
            return getItem(number, center, endIndex);
        }

        return null;
    }

    private boolean in(int number, Range<T> range) {
        return number <= range.end() && number >= range.start();
    }

}