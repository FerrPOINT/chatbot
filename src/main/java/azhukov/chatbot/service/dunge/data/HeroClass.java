package azhukov.chatbot.service.dunge.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public enum HeroClass {

    SAILOR("сэйлор пёс"),
    DEFENDER("защитник будки"),
    FAIRY("пёсья фея"),
    NECRO("собачий некромант"),
    NOBLE("благородный догги"),
    PRISONER("сорвавшийся с цепи заключенный"),
    SHAMAN("четверолапый шаман"),
    SAMURAI("догги самурай"),
    WEREWOLF("оборотень догэна"),
    ROGUE("шелудивый вор"),

    //
    ;

    public static final List<HeroClass> VALUES = List.of(values());

    private final String label;
}
