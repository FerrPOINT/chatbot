package azhukov.chatbot.service.dunge.data;

import lombok.Data;

import java.util.HashSet;
import java.util.Set;

import static azhukov.chatbot.constants.Constants.DUNGEON_MULTIPLIER;

@Data
public class BossInfo {

    private int stage;
    private String name;
    private String label;
    private String minionsName;
    private String minionsLabel;
    private HeroClass strong;
    private HeroClass weak;
    private int gotDamage;
    private Set<String> damagedHeroes = new HashSet<>();

    public int getMaxHp() {
        return stage * DUNGEON_MULTIPLIER;
    }

    public boolean isDead() {
        return gotDamage >= getMaxHp();
    }

    public void dealDamage(int damage) {
        gotDamage += Math.abs(damage);
    }

    public void heal(int heal) {
        gotDamage = Math.max(0, gotDamage - Math.abs(heal));
    }

    public int getCurrentHp() {
        return getMaxHp() - gotDamage;
    }

}
