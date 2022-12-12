package azhukov.chatbot.service.dunge.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum HeroClass {

    LALKA("лалка в матроске"),
    DEFENDER("защитник чата"),
    PIE("магический пирожок"),
    DARK("тёмная личность"),
    NUBAS("благородный нубаська"),
    ELF("голопопый эльф"),
    ORC("орк сморк"),
    HALFLING("босый полурослик"),
    WEREWOLF("оборотень догэна"),

    //
    ;

    private final String label;
}
