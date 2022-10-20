package azhukov.chatbot.service.macro;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum Macro {

    DOGGIE("{DOGGIE}"),
    PEKA("{PEKA}"),
    LICK("{LICK}"),
    PANTS("{PANTS}"),
    CAT_PISOS("{CAT_PISOS}"),
    PLEASURE("{PLEASURE}"),
    GUN("{GUN}"),

    //
    ;
    private final String macro;

}
