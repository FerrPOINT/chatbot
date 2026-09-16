package azhukov.chatbot.service.dunge.event;

import azhukov.chatbot.service.dunge.data.HeroDamage;
import azhukov.chatbot.service.dunge.service.BossService;
import azhukov.chatbot.service.dunge.service.HeroHealthService;
import azhukov.chatbot.service.weight.Weight;
import org.springframework.stereotype.Component;

@Component
public class BigHurtEvent extends HurtEvent {

    public BigHurtEvent(BossService bossService, HeroHealthService health) {
        super(bossService, health);
    }

    @Override
    HeroDamage getDamage() {
        return HeroDamage.BIG;
    }

    @Override
    public Weight getWeight() {
        return Weight.RARE;
    }

}
