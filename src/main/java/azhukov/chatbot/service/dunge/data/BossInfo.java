package azhukov.chatbot.service.dunge.data;

import lombok.Data;

import java.util.HashSet;
import java.util.Set;

import static azhukov.chatbot.constants.Constants.DUNGEON_MULTIPLIER;

@Data
public class BossInfo {

    private int stage;
    private int level;
    private String name;
    private String label;
    private String minionsName;
    private String minionsLabel;
    private HeroClass strong;
    private HeroClass weak;
    private int damageReceived;
    private Set<String> damagedHeroes = new HashSet<>();
    private Set<String> rewards = new HashSet<>();
    private int stealPercent;

    public int getMaxHp() {
        return level * DUNGEON_MULTIPLIER;
    }

    public boolean isDead() {
        return damageReceived >= getMaxHp();
    }

    public void dealDamage(int damage) {
        damageReceived += Math.abs(damage);
    }

    public void heal(int heal) {
        damageReceived = Math.max(0, damageReceived - Math.abs(heal));
    }

    public void resetOpponents() {
        damagedHeroes = new HashSet<>();
    }

    public int getCurrentHp() {
        return getMaxHp() - damageReceived;
    }

}
