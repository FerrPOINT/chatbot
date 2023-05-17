package azhukov.chatbot.service.dunge.event;

import azhukov.chatbot.service.dunge.data.HeroInfo;
import azhukov.chatbot.service.weight.Weight;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class MiddleShieldEvent implements DungeEvent {

    @Override
    public String handle(HeroInfo hero) {
        hero.setShield(hero.getShield() + 3);
        return "большой храм защитницы Догыни - героине былых лет. Помолившись вы чувствуете себя под защитой Догыни, ваша защита значительно возросла, текущий уровень защиты: " + hero.getShield() + ".";
    }

    @Override
    public Weight getWeight() {
        return Weight.LOW;
    }

}
