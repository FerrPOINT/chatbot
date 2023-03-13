package azhukov.chatbot.service.dunge.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum HeroDamage {

    NONE(0, "нет урона", "нет урона"),
    SLIGHT(1, "незначительный урон", "слегка ранен"),
    MEDIUM(2, "средний урон", "средне ранен"),
    BIG(3, "большой урон", "сильно ранен"),
    HUGE(4, "огромный урон", "очень даже ранен =)"),
    ALMOUST_DEAD(5, "громадный урон", "при смерти"),
    DEAD(6, "смертельный урон", "мертв"),

    //
    ;

    private static final HeroDamage[] RANGES;

    static {
        HeroDamage[] values = values();
        RANGES = new HeroDamage[values.length];
        for (HeroDamage value : values) {
            RANGES[value.getValue()] = value;
        }
    }

    private final int value;
    private final String label;
    private final String status;

    public static HeroDamage getByValue(int value) {
        return value >= RANGES.length || value < 0 ? null : RANGES[value];
    }

    public HeroDamage join(HeroDamage damage) {
        int newValue = Math.min(damage.getValue() + getValue(), RANGES.length - 1);
        return getByValue(newValue);
    }

}
