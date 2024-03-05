package azhukov.chatbot.service.dunge.event;

import azhukov.chatbot.service.dunge.data.HeroInfo;
import azhukov.chatbot.service.dunge.service.DungeonService;
import azhukov.chatbot.service.weight.Weight;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class StealEvent implements DungeEvent {

    @Override
    public String handle(HeroInfo hero) {
        String art = DungeonService.tryToSteal(15, hero);
        if (art == null) {
            hero.setExperience(hero.getExperience() + 500);
        }
        return "святилище Догги-Роги - лучшего воришки прошедших лет. " + (art != null ? ("Вы неправильно прочитали молитву и Рога ворует у вас артефакт: " + art) :
                "Вы правильно прочитали молитву и получаете дополнительный опыт");
    }

    @Override
    public Weight getWeight() {
        return Weight.RARE;
    }

}
