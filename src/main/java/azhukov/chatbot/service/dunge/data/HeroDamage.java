package azhukov.chatbot.service.dunge.data;

import azhukov.chatbot.util.Range;
import azhukov.chatbot.util.RangesContainer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public enum HeroDamage {

    NONE(0, 40, "нет урона"),
    SLIGHT(40, 70, "незначительный урон"),
    MEDIUM(70, 85, "средний урон"),
    HUGE(85, 95, "огромный урон"),
    DEAD(95, 100, "смертельный урон"),

    //
    ;

    private static final RangesContainer<HeroDamage> RANGES = new RangesContainer<>(Arrays.stream(values())
            .map(heroDamage -> new Range<>(heroDamage.getPercentStart(), heroDamage.getPercentEnd(), heroDamage))
            .collect(Collectors.toList())
    );

    private static int LAST_PERCENT = values()[values().length - 1].getPercentEnd();

    private final int percentStart;
    private final int percentEnd;
    private final String label;

    public static HeroDamage getByPercent(int percent) {
        return RANGES.getItem(percent);
    }

    public HeroDamage join(HeroDamage damage) {
        int percent = Math.min(LAST_PERCENT, this.percentStart + damage.getPercentStart());
        return getByPercent(percent);
    }

}
