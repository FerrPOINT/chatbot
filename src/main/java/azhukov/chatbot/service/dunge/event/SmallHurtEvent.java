package azhukov.chatbot.service.dunge.event;

import azhukov.chatbot.service.dunge.data.HeroDamage;
import azhukov.chatbot.service.dunge.data.HeroInfo;
import azhukov.chatbot.service.dunge.DungeonRandom;
import azhukov.chatbot.service.dunge.service.HeroHealthService;
import azhukov.chatbot.service.weight.Weight;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class SmallHurtEvent implements DungeEvent {

    private static final List<String> MESSAGES = List.of("булыжник. Вы ободрали коленку", "мелкую ловушку. Вы угодили в крупную мышеловку", "крысу. Вы были покусаны крыской");
    private final HeroHealthService health;
    private final DungeonRandom random;

    @Override
    public String handle(HeroInfo hero) {
        HeroDamage damage = HeroDamage.SLIGHT;
        HeroHealthService.DamageResult result = health.damage(hero, damage);
        return random.item(MESSAGES) + " и получили " + damage.getLabel() + ", статус: " + hero.getDamageGot().getStatus()
                + (result.isDied() ? ", PRESS F" : "");
    }

    @Override
    public Weight getWeight() {
        return Weight.MEDIUM;
    }

}
