package azhukov.chatbot.service.dunge.data;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
@Accessors(chain = true)
public class HeroInfo {

    private String name;
    private HeroClass type;
    private int experience;
    private int level;
    private List<Artifact> artifacts;
    private HeroDamage damageGot;
    private LocalDateTime deadTime;

    public int getAttack() {
        int result = level;
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
                            result += (result / 100 * modification.getValue());
                        }
                    }
                }
            }
        }
        return result;
    }


}
