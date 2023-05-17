package azhukov.chatbot.service.weight;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Weight {

    RARE(1),
    LOW(2),
    MEDIUM(3),
    HIGH(4),
    HIGHEST(5)

    //
    ;
    @Getter
    private final int value;
}
