package azhukov.chatbot.service.macro;

import java.util.Arrays;
import java.util.Map;

import static azhukov.chatbot.service.macro.Macro.*;

public class MacrosDictionary {

    public static final Map<String, String> GG_DICTIONARY = Map.of(
            DOGGIE.getMacro(), ":doggie:",
            PEKA.getMacro(), ":peka:",
            LICK.getMacro(), ":tanushkavl11:",
            PANTS.getMacro(), ":tanushkavl29:",
            CAT_PISOS.getMacro(), ":tanushkavl1:",
            PLEASURE.getMacro(), ":tanushkavl3:",
            GUN.getMacro(), ":tanushkavl15:",
            SIGH.getMacro(), ":sigh:"
    );

    public static final Map<String, String> DISCORD_DICTIONARY = Map.of(
            DOGGIE.getMacro(), ":dog:",
            PEKA.getMacro(), ":smirk:",
            LICK.getMacro(), ":tongue:",
            PANTS.getMacro(), ":nerd:",
            CAT_PISOS.getMacro(), ":smirk_cat:",
            PLEASURE.getMacro(), ":heart_eyes:",
            GUN.getMacro(), ":gun:",
            SIGH.getMacro(), ":eyes:"
    );

    public static final Map<String, String> TWITCH_DICTIONARY = Map.of(
            DOGGIE.getMacro(), "OhMyDog",
            PEKA.getMacro(), "LUL",
            LICK.getMacro(), "BrokeBack",
            PANTS.getMacro(), "PansexualPride",
            CAT_PISOS.getMacro(), "DxCat",
            PLEASURE.getMacro(), "<3",
            GUN.getMacro(), "twitchRaid",
            SIGH.getMacro(), "WutFace"
    );

    static {
        // sanity check
        Macro[] values = values();

        if (!Arrays.stream(values).map(Macro::getMacro).allMatch(GG_DICTIONARY::containsKey)) {
            throw new IllegalArgumentException("GG_DICTIONARY not full");
        }

        if (!Arrays.stream(values).map(Macro::getMacro).allMatch(DISCORD_DICTIONARY::containsKey)) {
            throw new IllegalArgumentException("DISCORD_DICTIONARY not full");
        }

        if (!Arrays.stream(values).map(Macro::getMacro).allMatch(TWITCH_DICTIONARY::containsKey)) {
            throw new IllegalArgumentException("DISCORD_DICTIONARY not full");
        }
    }


}
