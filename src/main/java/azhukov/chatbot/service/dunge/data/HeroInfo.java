package azhukov.chatbot.service.dunge.data;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
@Accessors(chain = true)
public class HeroInfo {

    private String name;
    private HeroClass type;
    private int experience;
    private List<Artifact> artifacts;
    private HeroDamage damageGot;
    private LocalDateTime deadTime;
    private int shield;

    public int getLevel() {
        return (experience / 1000) + 1;
    }

    public int getAttack(BossInfo boss) {
        int result = getLevel() * 10;
        if (artifacts != null) {
            for (Artifact artifact : artifacts) {
                if (artifact.getModifications() != null) {
                    for (Modificator modification : artifact.getModifications()) {
                        if (modification.getModificationType() == ModificationType.ATTACK_CHANGE) {
                            result += modification.getValue();
                        }
                    }
                }
            }
            for (Artifact artifact : artifacts) {
                if (artifact.getModifications() != null) {
                    for (Modificator modification : artifact.getModifications()) {
                        if (modification.getModificationType() == ModificationType.ATTACK_PERCENT) {
                            result += (int) (((double) result / 100D) * (double) modification.getValue());
                        }
                    }
                }
            }
        }
        if (boss.getWeak() == type) {
            result *= 2;
        }
        return result;
    }

    public void resetShield() {
        setShield(getArtifactsMaxShieldValue());
    }

    private int getArtifactsMaxShieldValue() {
        int max = 0;
        if (artifacts != null) {
            for (Artifact artifact : artifacts) {
                if (artifact.getModifications() != null) {
                    for (Modificator modification : artifact.getModifications()) {
                        if (modification.getModificationType() == ModificationType.DAILY_GUARD) {
                            max = Math.max(max, modification.getValue());
                        }
                    }
                }
            }
        }
        return max;
    }

    public void addArtifact(Artifact artifact) {
        if (artifacts == null) {
            artifacts = new ArrayList<>();
        }
        artifacts.add(artifact);
        resetShield();
    }

}
