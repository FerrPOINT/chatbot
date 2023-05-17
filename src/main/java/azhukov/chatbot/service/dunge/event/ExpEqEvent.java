package azhukov.chatbot.service.dunge.event;

import azhukov.chatbot.service.dunge.data.HeroInfo;
import azhukov.chatbot.service.weight.Weight;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class ExpEqEvent implements DungeEvent {

    @Override
    public String handle(HeroInfo hero) {
        int level = hero.getLevel();
        int expChange = level > 20 ? -1000 : 1000;
        hero.setExperience(hero.getExperience() + expChange);
        return "святилище Догги-уравнителя - сущность управляющую опытом. " + (expChange > 0 ?
                "Нуждающимся героям уравнитель дарит дополнительный уровень" :
                "У прокачаных героев уравнитель забирает уровень в пользу бедных");
    }

    @Override
    public Weight getWeight() {
        return Weight.MEDIUM;
    }

}
