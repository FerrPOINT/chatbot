package azhukov.chatbot.service.dunge.data;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Accessors(chain = true)
@Data
public class FightResult {

    private int damageDone;
    private HeroDamage damageGet;
    private Artifact artPrize;
    private int moneyPrize;
    private int exp;
    private int kills;
    private LocalDateTime fightTime;
    private BossInfo boss;
    private HeroInfo hero;

}
