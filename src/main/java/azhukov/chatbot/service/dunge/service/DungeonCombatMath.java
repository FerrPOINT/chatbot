package azhukov.chatbot.service.dunge.service;

import lombok.experimental.UtilityClass;

@UtilityClass
public class DungeonCombatMath {
    public long normalizedXp(long realDamage, long heroLevel, int rounds, int receivedDamageLevel,
                             int bossStage, int expBuffPercent, int artifactXpPercent) {
        long damageXp = Math.max(0L, realDamage) / Math.max(1L, heroLevel);
        long riskPerRound = 10L * Math.max(0, receivedDamageLevel)
                + Math.max(0L, 100L - bossStage - heroLevel);
        long base = Math.max(1L, DungeonNumbers.add(damageXp, DungeonNumbers.multiply(Math.max(0, rounds), riskPerRound)));
        long percent = (long) Math.max(0, expBuffPercent) + Math.max(0, artifactXpPercent);
        return Math.max(0L, DungeonNumbers.multiplyDivide(base, 100L + percent, 100L));
    }

    public long ceilDiv(long dividend, long divisor) {
        if (dividend <= 0) return 0;
        if (divisor <= 0) throw new IllegalArgumentException("divisor must be positive");
        return 1L + (dividend - 1L) / divisor;
    }
}
