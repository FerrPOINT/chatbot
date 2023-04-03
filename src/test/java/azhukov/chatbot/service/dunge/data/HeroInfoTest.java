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

    @Test
    void testAttack() {
        HeroInfo heroInfo = new HeroInfo();
        heroInfo.setType(HeroClass.LALKA);
        heroInfo.addArtifact(new Artifact("1", "", "", List.of(new Modificator(ModificationType.ATTACK_CHANGE, 10))));
        heroInfo.addArtifact(new Artifact("2", "", "", List.of(new Modificator(ModificationType.ATTACK_PERCENT, 50))));
        assertEquals(30, heroInfo.getAttack(new BossInfo()));
    }

}