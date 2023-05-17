package azhukov.chatbot.service.dunge.event;

import azhukov.chatbot.service.dunge.data.HeroInfo;
import azhukov.chatbot.service.weight.Weight;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class CritEvent implements DungeEvent {

    @Override
    public String handle(HeroInfo hero) {
        hero.setCrit(hero.getCrit() + 1);
        return "святилище Догги-берсерка - героя прошлых лет. Помолившись вы чувствуете как ваш крит увеличился";
    }

    @Override
    public Weight getWeight() {
        return Weight.MEDIUM;
    }

}
