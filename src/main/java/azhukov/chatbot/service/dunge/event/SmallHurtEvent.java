package azhukov.chatbot.service.dunge.event;

import azhukov.chatbot.service.dunge.data.HeroDamage;
import azhukov.chatbot.service.dunge.data.HeroInfo;
import azhukov.chatbot.service.util.Randomizer;
import azhukov.chatbot.service.weight.Weight;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class SmallHurtEvent implements DungeEvent {

    private static final List<String> MESSAGES = List.of("булыжник. Вы ободрали коленку", "мелкую ловушку. Вы не смогли увенуться от фантомного пендаля", "крысу. Вы были покусаны крыской");

    @Override
    public String handle(HeroInfo hero) {
        HeroDamage damage = HeroDamage.SLIGHT;
        hero.setDamageGot(hero.getDamageGot().join(damage));
        return Randomizer.getRandomItem(MESSAGES) + " и получили " + damage.getLabel() + ", вместе с этим статус: " + hero.getDamageGot().getStatus();
    }

    @Override
    public Weight getWeight() {
        return Weight.MEDIUM;
    }

}
