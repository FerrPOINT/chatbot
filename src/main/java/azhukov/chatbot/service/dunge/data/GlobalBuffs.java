package azhukov.chatbot.service.dunge.data;

import lombok.Data;

@Data
public class GlobalBuffs {
    private int attackStacks;
    private int shieldStacks;
    private int expStacks;

    public int attackPercent() { return Math.min(3, Math.max(0, attackStacks)) * 20; }
    public int shield() { return Math.min(3, Math.max(0, shieldStacks)) * 2; }
    public int expPercent() { return Math.min(3, Math.max(0, expStacks)) * 20; }
    public void addAttack() { attackStacks = Math.min(3, attackStacks + 1); }
    public void addShield() { shieldStacks = Math.min(3, shieldStacks + 1); }
    public void addExp() { expStacks = Math.min(3, expStacks + 1); }
    public void clear() { attackStacks = shieldStacks = expStacks = 0; }
}
