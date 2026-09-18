package azhukov.chatbot.service.dunge.data;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class DungeonOperation {
    public enum Status { PREPARED, COMPLETED }
    public enum Type { FIGHT, ABILITY, ABILITY_DAMAGE, RUSH, SIEGE, HEAL, RANSOM, ARTIFACT_GRANT, EVENT, DEATH, REVIVE, RESET, REWARD }

    private String id;
    private Type type;
    private Status status = Status.PREPARED;
    private String hero;
    private String targetHero;
    private String bossInstance;
    private long amount;
    private long secondaryAmount;
    private long coinReward;
    private int incomingDamage;
    private int temporaryShield;
    private int rounds;
    private int attackBuffPercent;
    private int experienceBuffPercent;
    private int shieldBuff;
    private String artifactId;
    private String payload;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
