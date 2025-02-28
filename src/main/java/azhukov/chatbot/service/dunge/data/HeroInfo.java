package azhukov.chatbot.service.dunge.data;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.collections4.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
    private float crit;
    private int events;
    private boolean specialAbilityUsed;
    private int rebornPercentage;

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
            result += result / 2;
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

    public void addShields(int shields) {
        setShield(getShield() + shields);
    }

    public void heal(int heal) {
        setDamageGot(HeroDamage.getByValue(getDamageGot().getValue() + heal));
    }

    public void addExp(int exp) {
        experience += exp;
    }

    public boolean isDead() {
        return damageGot == HeroDamage.DEAD;
    }

    public boolean hasArtifacts() {
        return CollectionUtils.isNotEmpty(artifacts);
    }

    public boolean hasArtifact(Artifact artifact) {
        return hasArtifacts() && artifacts.stream().anyMatch(a -> a.getId().equals(artifact.getId()));
    }

    public boolean removeArtifact(Artifact artifact) {
        return hasArtifacts() && artifacts.removeIf(a -> a.getId().equals(artifact.getId()));
    }

    public void addArtifact(Artifact artifact) {
        if (artifact == null) {
            return;
        }
        if (artifacts == null) {
            artifacts = new ArrayList<>();
        }
        if (artifacts.stream().noneMatch(artifact1 -> Objects.equals(artifact1.getId(), artifact.getId()))) {
            artifacts.add(artifact);
            resetShield();
        }
    }

}
