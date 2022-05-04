package azhukov.chatbot.service.pet;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum LifecycleStage {

    HOMELESS("Ваша собаня шарится по мусоркам, так как вынуждена делать это ради пропитания"),
    WERY_HUNGRY("Ваш пёс голоден и давненько не ел"),
    HUNGRY("Ваша псинка слегка голодная"),
    FED("Ваш пёсик накормлен, но уже подумывает о еде"),
    WELL_FED("Догэн полностью накормлен, вуф"),

    ;

    private final String message;

    private final static LifecycleStage MIN = values()[0];
    private final static LifecycleStage MAX = values()[values().length - 1];

    public LifecycleStage offset(int amount) {
        final LifecycleStage[] all = values();
        final int current = ordinal();
        int result = current + amount;
        result = Math.min(result, all.length - 1);
        result = Math.max(result, 0);
        return all[result];
    }

    public boolean isMax() {
        return MAX == this;
    }

    public boolean isMin() {
        return MIN == this;
    }

}
