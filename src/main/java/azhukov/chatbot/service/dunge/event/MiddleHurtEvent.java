package azhukov.chatbot.service.dunge.event;

import azhukov.chatbot.service.dunge.data.HeroDamage;
import azhukov.chatbot.service.dunge.service.BossService;
import azhukov.chatbot.service.weight.Weight;
import org.springframework.stereotype.Component;

@Component
public class MiddleHurtEvent extends HurtEvent {

    public MiddleHurtEvent(BossService bossService) {
        super(bossService);
    }

    @Override
    HeroDamage getDamage() {
        return HeroDamage.MEDIUM;
    }

    @Override
    public Weight getWeight() {
        return Weight.LOW;
    }

}
