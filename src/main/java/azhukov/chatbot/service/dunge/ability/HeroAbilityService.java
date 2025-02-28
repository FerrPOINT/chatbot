package azhukov.chatbot.service.dunge.ability;

import azhukov.chatbot.service.dunge.data.*;
import azhukov.chatbot.service.dunge.service.BossService;
import azhukov.chatbot.service.dunge.service.HeroInfoService;
import azhukov.chatbot.service.util.Randomizer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class HeroAbilityService {

    private static final int ATTACK_BUFF = 20;
    private static final int EXP_BUFF = 20;
    private static final int SHIELD_BUFF = 2;

    private final HeroInfoService heroInfoService;
    private final BossService bossService;

    @Getter
    private AbilitiesData nextHeroBuffs = new AbilitiesData();

    public synchronized void resetNextHeroAbilities() {
        nextHeroBuffs = new AbilitiesData();
    }

    public String useHeroAbility(HeroInfo targetHero, HeroInfo previousHero) {
        return applySpecialAbility(targetHero, targetHero.getType(), previousHero);
    }

    private synchronized String applySpecialAbility(HeroInfo targetHero, HeroClass targetHeroType, HeroInfo previousHero) {
        if (targetHero.isSpecialAbilityUsed()) {
            return "Способность уже использована и может быть использована только раз в день.";
        }

        switch (targetHeroType) {
            case SAILOR:
                updateAbilityData(targetHero);
                nextHeroBuffs.setAttackUpdate(nextHeroBuffs.getAttackUpdate() + ATTACK_BUFF);
                return targetHeroType.getAbilityName() + ": увеличена атака для следующего спустившегося в данж";
            case DEFENDER:
                updateAbilityData(targetHero, heroInfo -> heroInfo.addShields(2));
                // Метод для добавления щита в 2 единицы
                return targetHeroType.getAbilityName() + ": добавлено 3 единицы щита";
            case FAIRY:
                if (previousHero != null) {
                    updateAbilityData(targetHero);
                    updateAbilityData(previousHero, heroInfo -> heroInfo.heal(2)); // Метод для лечения на 2 единицы
                    return targetHeroType.getAbilityName() + ": исцелен " + previousHero.getName() + " на 2 единицы здоровья";
                }
                return targetHeroType.getAbilityName() + ": предыдущий герой не найден.";
            case NECRO:
                if (previousHero != null) {
                    if (previousHero.isDead()) {
                        // Метод для воскрешения героя с половинным уровнем
                        updateAbilityData(targetHero);
                        updateAbilityData(previousHero, heroInfo -> {
                            heroInfo.setDamageGot(HeroDamage.MEDIUM);
                            heroInfo.setDeadTime(null);
                            heroInfo.setExperience(targetHero.getExperience() / 2);
                        });
                        return targetHeroType.getAbilityName() + ": воскрешен " + previousHero.getName();
                    } else {
                        return targetHeroType.getAbilityName() + ": невозможно воскресить, " + previousHero.getName() + " не мертв";
                    }
                }
                return targetHeroType.getAbilityName() + ": предыдущий герой не найден.";
            case NOBLE:
                updateAbilityData(targetHero);
                nextHeroBuffs.setExpUpdate(nextHeroBuffs.getExpUpdate() + EXP_BUFF);
                return targetHeroType.getAbilityName() + ": увеличен получаемый опыт для следующего героя" + targetHero.getName();
            case PRISONER:
                updateAbilityData(targetHero, heroInfo -> heroInfo.setRebornPercentage(heroInfo.getRebornPercentage() + 50));
                // Специальная логика выживания при смертельном исходе будет обработана отдельно
                return targetHeroType.getAbilityName() + ": еще один день, когда ты может быть спасешься, находясь на волосок от смерти";
            case SHAMAN:
                nextHeroBuffs.setExpUpdate(nextHeroBuffs.getShieldUpdate() + SHIELD_BUFF);
                updateAbilityData(targetHero);
                return targetHeroType.getAbilityName() + ": добавлено 2 единицы щита для следующего героя";
            case SAMURAI:
                BossInfo boss = bossService.getCurrentBoss(); // Предполагается существование BossService
                boss.dealDamage(targetHero.getAttack(boss)); // Метод для нанесения урона боссу
                updateAbilityData(targetHero);
                return targetHeroType.getAbilityName() + ": скрытный удар по боссу " + boss.getName();
            case WEREWOLF:
                HeroClass randomClass = HeroClass.WEREWOLF;
                while (randomClass == HeroClass.WEREWOLF) {
                    randomClass = HeroClass.getRandomClass();
                }
                return targetHeroType.getAbilityName() + ": использована рандомная способность. " + applySpecialAbility(targetHero, randomClass, previousHero);
            case ROGUE:
                if (previousHero != null) {
                    if (previousHero.hasArtifacts()) {
                        if (Randomizer.tossCoin()) {
                            updateAbilityData(targetHero);
                            return targetHeroType.getAbilityName() + ": не удалось похитить предмет у " + previousHero.getName();
                        }
                        Artifact stolenItem = Randomizer.getRandomItem(previousHero.getArtifacts()); // Метод для кражи предмета
                        boolean hasArtifact = targetHero.hasArtifact(stolenItem);
                        if (hasArtifact) {
                            updateAbilityData(previousHero, heroInfo -> heroInfo.removeArtifact(stolenItem));
                            updateAbilityData(targetHero, heroInfo -> heroInfo.setExperience(1000));
                            return targetHeroType.getAbilityName() + ": украден уже имеющийся предмет у " + previousHero.getName() + " и преобразован в 1000 опыта";
                        } else {
                            updateAbilityData(targetHero, heroInfo -> targetHero.addArtifact(stolenItem));
                            return targetHeroType.getAbilityName() + ": украден предмет у " + previousHero.getName();
                        }
                    } else {
                        return targetHeroType.getAbilityName() + ": у " + previousHero.getName() + " нет предметов для кражи";
                    }
                } else {
                    return targetHeroType.getAbilityName() + ": предыдущий герой не найден.";
                }
            default:
                return "Способность не определена.";
        }
    }


    public void updateAbilityData(HeroInfo info) {
        updateAbilityData(info, null);
    }

    public void updateAbilityData(HeroInfo info, Consumer<HeroInfo> updater) {
        heroInfoService.update(info, heroInfo -> {
            if (updater != null) {
                updater.accept(heroInfo);
            }
            heroInfo.setSpecialAbilityUsed(true);
        });
    }

}
