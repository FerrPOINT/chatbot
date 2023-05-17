package azhukov.chatbot.service.dunge.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum HeroDamage {

    //    SHIELD(-1, "защита", "защита"),
    NONE(0, "нет урона", "нет урона"),
    SLIGHT(1, "незначительный урон", "слегка ранен"),
    MEDIUM(2, "средний урон", "средне ранен"),
    BIG(3, "большой урон", "сильно ранен"),
    HUGE(4, "огромный урон", "ужасно ранен"),
    ALMOUST_DEAD(5, "громадный урон", "при смерти"),
    DEAD(6, "смертельный урон", "мертв"),

    //
    ;

    private static final HeroDamage[] RANGES;

    static {
        HeroDamage[] values = values();
        RANGES = new HeroDamage[(int) Arrays.stream(values).filter(heroDamage -> heroDamage.getValue() >= 0).count()];
        for (HeroDamage value : values) {
            if (value.getValue() >= 0) {
                RANGES[value.getValue()] = value;
            }
        }
    }

    private final int value;
    private final String label;
    private final String status;

    public static HeroDamage getByValue(int value) {
        if (value < 0) {
            return NONE;
        }
        if (value >= RANGES.length) {
            return DEAD;
        }
        return RANGES[value];
    }

    public HeroDamage join(HeroDamage damage) {
        if (damage.getValue() < 0) {
            damage = NONE;
        }
        if (this.getValue() < 0) {
            return damage;
        }
        int newValue = Math.min(damage.getValue() + getValue(), RANGES.length - 1);
        return getByValue(newValue);
    }

    public HeroDamage heal(HeroDamage damage) {
        if (damage.getValue() < 0) {
            damage = NONE;
        }
        if (this.getValue() < 0) {
            return damage;
        }
        int newValue = Math.min(getValue() - damage.getValue(), RANGES.length - 1);
        return getByValue(newValue);
    }

}
