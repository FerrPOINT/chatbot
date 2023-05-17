package azhukov.chatbot.service.dunge.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

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

    public static final List<HeroClass> VALUES = List.of(values());

    private final String label;
}
