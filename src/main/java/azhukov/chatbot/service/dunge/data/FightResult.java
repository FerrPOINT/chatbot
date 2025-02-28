package azhukov.chatbot.service.dunge.data;

import azhukov.chatbot.service.dunge.ability.AbilitiesData;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Accessors(chain = true)
@Data
public class FightResult {

    private int damageDone;
    private HeroDamage damageReceived;
    private Artifact artPrize;
    private int moneyPrize;
    private int exp;
    private int kills;
    private LocalDateTime fightTime;
    private BossInfo boss;
    private HeroInfo hero;
    private int crit;
    private int shieldSpent;
    private int fightsNumber;
    private String stealArt;
    private boolean immunity;
    private AbilitiesData nextHeroBuffs;
    private boolean reborn;

}
