package azhukov.chatbot.service.dunge.event;

import azhukov.chatbot.service.dunge.data.HeroClass;
import azhukov.chatbot.service.dunge.data.HeroInfo;
import azhukov.chatbot.service.util.Randomizer;
import azhukov.chatbot.service.weight.Weight;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class ClassChangeEvent implements DungeEvent {

    @Override
    public String handle(HeroInfo hero) {
        HeroClass newType = hero.getType();
        HeroClass oldType = hero.getType();
        while (newType == oldType) {
            newType = Randomizer.getRandomItem(HeroClass.VALUES);
        }
        hero.setType(newType);
        return "алтарь со странным зеркалом, чем больше вы смотрели в него, тем больше вы превращались в кого-то другого. В прошлом " + oldType.getLabel() + " изменился до неузнаваемости и теперь вы " + newType.getLabel() + ". Кто знает, хорошо это или плохо";
    }

    @Override
    public Weight getWeight() {
        return Weight.RARE;
    }

}
