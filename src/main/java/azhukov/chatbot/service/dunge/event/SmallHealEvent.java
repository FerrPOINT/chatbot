package azhukov.chatbot.service.dunge.event;

import azhukov.chatbot.service.dunge.data.HeroDamage;
import azhukov.chatbot.service.dunge.data.HeroInfo;
import azhukov.chatbot.service.weight.Weight;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class SmallHealEvent implements DungeEvent {

    @Override
    public String handle(HeroInfo hero) {
        HeroDamage origDamage = hero.getDamageGot();
        if (origDamage != HeroDamage.NONE) {
            hero.setDamageGot(hero.getDamageGot().heal(HeroDamage.SLIGHT));
        } else {
            hero.setShield(hero.getShield() + 1);
        }
        return origDamage != HeroDamage.NONE ?
                "разбитую бутылку лечебного зелья. Получилось слизать остатки и залечить мелкую рану, общий статус: " + hero.getDamageGot().getStatus() + "." :
                "святилище защитницы Догыни - героине прошлых лет. Помолившись вы чувствуете как ваша защита возросла, текущий уровень защиты: " + hero.getShield() + ".";
    }

    @Override
    public Weight getWeight() {
        return Weight.HIGH;
    }

}
