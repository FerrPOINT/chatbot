package azhukov.chatbot.service.dunge.event;

import azhukov.chatbot.service.dunge.data.HeroDamage;
import azhukov.chatbot.service.dunge.service.BossService;
import azhukov.chatbot.service.weight.Weight;
import org.springframework.stereotype.Component;

@Component
public class BigHurtEvent extends HurtEvent {

    public BigHurtEvent(BossService bossService) {
        super(bossService);
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
