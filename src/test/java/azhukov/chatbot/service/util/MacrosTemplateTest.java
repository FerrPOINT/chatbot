package azhukov.chatbot.service.util;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MacrosTemplateTest {

    @Test
    void test() {
        MacrosTemplate macrosTemplate = new MacrosTemplate("yoyo{MACRO1}texttext%%MACRO2%%aa", Set.of("{MACRO1}", "%%MACRO2%%"));

        String result = macrosTemplate.compileString(Map.of(
                "{MACRO1}", () -> "value1",
                "%%MACRO2%%", () -> "value2"
        ));

        assertEquals("yoyovalue1texttextvalue2aa", result);

        macrosTemplate = new MacrosTemplate("{MACRO1}texttext%%MACRO2%%", Set.of("{MACRO1}", "%%MACRO2%%"));

        result = macrosTemplate.compileString(Map.of(
                "{MACRO1}", () -> "value1",
                "%%MACRO2%%", () -> "value2"
        ));

        assertEquals("value1texttextvalue2", result);
    }


}