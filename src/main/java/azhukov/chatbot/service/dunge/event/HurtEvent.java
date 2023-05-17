package azhukov.chatbot.service.dunge.event;

import azhukov.chatbot.service.dunge.data.BossInfo;
import azhukov.chatbot.service.dunge.data.HeroDamage;
import azhukov.chatbot.service.dunge.data.HeroInfo;
import azhukov.chatbot.service.dunge.service.BossService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class HurtEvent implements DungeEvent {

    private final BossService bossService;

    @Override
    public String handle(HeroInfo hero) {
        HeroDamage damage = getDamage();
        int shield = hero.getShield();
        int shieldSpend = 0;
        if (shield > 0) {
            if (shield <= damage.getValue()) {
                shieldSpend = shield;
                shield = 0;
                damage = HeroDamage.getByValue(damage.getValue() - shield);
            } else {
                shield -= damage.getValue();
                shieldSpend = damage.getValue();
                damage = HeroDamage.NONE;
            }
            hero.setShield(shield);
        }

        hero.setDamageGot(hero.getDamageGot().join(damage));
        BossInfo currentBoss = bossService.getCurrentBoss();
        String enemies = currentBoss == null ? "чертята-поросята из подземелья" : currentBoss.getMinionsLabel();
        return "случайных противников. Вас подкараулили " + enemiesModifier() + " " + enemies + " и нанесли вам " + getDamage().getLabel() + ", " +
                (hero.getDamageGot() == HeroDamage.DEAD ? "вы не пережили этой схватки PRESS F" : ((shieldSpend > 0 ? "брони потеряно: " + shieldSpend + ", " : "") + "вы сбежали со статусом: " + hero.getDamageGot().getStatus()));
    }

    private String enemiesModifier() {
        HeroDamage damage = getDamage();
        return damage.getValue() <= HeroDamage.MEDIUM.getValue() ? "мелкие" : "жирные";
    }

    abstract HeroDamage getDamage();

}
