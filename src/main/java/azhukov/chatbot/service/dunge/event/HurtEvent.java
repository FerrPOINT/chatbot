package azhukov.chatbot.service.dunge.event;

import azhukov.chatbot.service.dunge.data.HeroDamage;
import azhukov.chatbot.service.dunge.data.HeroInfo;
import azhukov.chatbot.service.dunge.service.BossService;
import azhukov.chatbot.service.dunge.service.HeroHealthService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class HurtEvent implements DungeEvent {

    private final BossService bossService;
    private final HeroHealthService health;

    @Override
    public String handle(HeroInfo hero) {
        HeroHealthService.DamageResult result = health.damage(hero, getDamage());
        var currentBoss = bossService.getCurrentBoss();
        String enemies = currentBoss == null ? "чертята-поросята из подземелья" : currentBoss.getMinionsLabel();

        return "случайных противников. Вас подкараулили " + enemiesModifier() + " " + enemies + " и нанесли вам " + getDamage().getLabel() + ", " +
                (result.isDied() ? "вы не пережили этой схватки PRESS F" : ((result.isSaved() ? "вы чудом спаслись, " : "") +
                        ((result.getAbsorbed() > 0 ? "брони потеряно: " + result.getAbsorbed() + ", " : "") + "вы сбежали со статусом: " + hero.getDamageGot().getStatus())));
    }

    private String enemiesModifier() {
        HeroDamage damage = getDamage();
        return damage.getValue() <= HeroDamage.MEDIUM.getValue() ? "мелкие" : "жирные";
    }

    abstract HeroDamage getDamage();

}
