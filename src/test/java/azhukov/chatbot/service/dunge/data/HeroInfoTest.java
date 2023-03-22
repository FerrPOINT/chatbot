package azhukov.chatbot.service.dunge.data;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HeroInfoTest {
    @Test
    void test() {
        HeroInfo heroInfo = new HeroInfo();
        heroInfo.addArtifact(new Artifact("", "", "", List.of(new Modificator(ModificationType.DAILY_GUARD, 10))));
        assertEquals(10, heroInfo.getShield());
    }
}