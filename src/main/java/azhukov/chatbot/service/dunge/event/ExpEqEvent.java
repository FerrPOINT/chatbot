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
        long level = hero.getLevel();
        long expChange = level > 20 ? -1000 : 1000;
        hero.addExp(expChange);
        return "святилище Догги-уравнителя - сущность управляющую опытом. " + (expChange > 0 ?
                "Нуждающимся героям уравнитель дарит дополнительный уровень" :
                "У прокачанных героев уравнитель забирает уровень в пользу бедных");
    }

    @Override
    public Weight getWeight() {
        return Weight.MEDIUM;
    }

}
