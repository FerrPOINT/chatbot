package azhukov.chatbot.service.pet;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum LifecycleStage {

    HOMELESS("Ваша собаня шарится по мусоркам, так как вынуждена делать это ради пропитания"),
    MAD("Ваша собаня сбежала от вас"),
    WERY_HUNGRY("Ваш пёс голоден и у него урчит в животе"),
    HUNGRY("Ваша псинка слегка голодная"),
    FED("Ваш пёсик накормлен, но уже подумывает о еде"),
    HAPPY("Собаня счастлив и не задумывается о еде"),
    WELL_FED("Догэн полностью накормлен, вуф"),

    ;

    public static final LifecycleStage[] VALUES = values();

    private final String message;

    private final static LifecycleStage MIN = VALUES[0];
    private final static LifecycleStage MAX = VALUES[VALUES.length - 1];

    public LifecycleStage offset(int amount) {
        final int current = ordinal();
        int result = current + amount;
        result = Math.min(result, VALUES.length - 1);
        result = Math.max(result, 0);
        return VALUES[result];
    }

    public boolean isMax() {
        return MAX == this;
    }

    public boolean isMin() {
        return MIN == this;
    }

}
