package azhukov.chatbot.service.dunge.service;

import azhukov.chatbot.dto.ChatRequest;
import azhukov.chatbot.service.ArticfactService;
import azhukov.chatbot.service.dunge.data.*;
import azhukov.chatbot.service.util.Randomizer;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static azhukov.chatbot.service.dunge.data.HeroDamage.*;

@Service
@RequiredArgsConstructor
public class DungeonService {

    private final HeroInfoService heroInfoService;
    private final BossService bossService;
    private final ArticfactService articfactService;

    //every day at 3:00
    @Scheduled(cron = "0 0 3 * * ?")
    void healScheduled() {
        heroInfoService.healAll();
        bossService.updateCurrentBoss(bossInfo -> {
            int threshold = bossInfo.getMaxHp() / 2 - bossInfo.getDamageReceived();
            bossInfo.heal(threshold <= 0 ? Math.abs(threshold) : 0);
            bossInfo.resetOpponents();
        });
    }

    public String getHeroStats(ChatRequest request) {
        String userName = request.getUserName();
        HeroInfo current = heroInfoService.getCurrent(userName);
        return current != null ? getUserStats(userName, current) : "Вас нет в мире живых, но вы уже готовы возродиться героем!";
    }

    public synchronized String getDungeonResponse(ChatRequest request) {
        String userName = request.getUserName();
        HeroInfo current = heroInfoService.getCurrent(userName);
        if (current == null) {
            current = heroInfoService.createNew(userName);
            return "Новый герой - " + getUserShortStats(userName, current) + " уже готов спуститься в подземелье !данж !стата !босс {DOGGIE}";
        }
        if (current.getDamageGot() == DEAD || current.getDeadTime() != null) {
            return "Вы мертвы, ваша душа блуждает в другом мире, подождите чтобы вновь обрести физическую форму! {DOGGIE}";
        }
        FightResult fight = fight(userName, current);

        if (fight == null) {
            return "Данж разорён, тут совсем пусто, подождём пока тут поселится новый злодей! {DOGGIE}";
        }

        return getBossMessage(fight) + ". " + getHeroMessage(fight) + getRewardsMessage(fight) + " {DOGGIE}";
    }

    private String getRewardsMessage(FightResult fight) {
        if (fight.getBoss().isDead() && CollectionUtils.isNotEmpty(fight.getBoss().getRewards())) {
            List<Artifact> rewards = fight.getBoss().getRewards().stream().map(articfactService::getById).filter(Objects::nonNull).collect(Collectors.toList());
            if (!rewards.isEmpty()) {
                for (String damagedHero : fight.getBoss().getDamagedHeroes()) {
                    heroInfoService.update(damagedHero, heroInfo -> {
                        for (Artifact reward : rewards) {
                            heroInfo.addArtifact(reward);
                        }
                    });
                }
                return ". Все кто участвовал и выжил получают " + fight.getBoss().getRewards().stream().collect(Collectors.joining(", "));
            }
        }
        return "";
    }

    public void resetAccs() {
        heroInfoService.reset();
    }

    public void resetBoss() {
        bossService.reset();
    }

    public void resetCurrAcc(String name) {
        heroInfoService.clean(name);
    }

    public void resetCurrBoss() {
        bossService.resetCurrentBoss();
    }

    private String getBossMessage(FightResult fight) {
        BossInfo boss = fight.getBoss();
        HeroInfo hero = fight.getHero();
        return "Данж " + hero.getName() + " " + fight.getHero().getType().getLabel() + " VS " + boss.getName() + " " + fight.getBoss().getLabel() + ". Ваш урон " + fight.getDamageDone() + (fight.getCrit() > 1 ? ", КРИТ Х" + fight.getCrit() : "") + (boss.isDead() ? ", ВЫ ЗАВАЛИЛИ БОССА, ПОЗДРАВЛЯЕМ!" : ", у босса осталось " + boss.getCurrentHp() + " HP");
    }

    private String getHeroMessage(FightResult fight) {
        BossInfo boss = fight.getBoss();
        HeroInfo hero = fight.getHero();
        HeroDamage damageGet = fight.getDamageReceived();
        if (hero.getDamageGot() == DEAD) {
            return "Вы рискнули напасть на " + boss.getName() + ", но он вам нанес " + damageGet.getLabel() + ", вместе с этим вы получили смертельный урон, PRESS F!";
        }
        return (damageGet.getValue() <= 0 ? "Вы всё задоджили " :
                "Вам досталось " + damageGet.getLabel()) +
                (fight.getShieldSpent() > 0 ? ", " + " брони потрачено: " + fight.getShieldSpent() : "") +
                ", получено опыта: " + fight.getExp() + ", общий статус: " + hero.getDamageGot().getStatus() + ", " +
                (hero.getDamageGot().getValue() < HeroDamage.BIG.getValue() ? "не опасно"
                        : "сегодня уже опасно, рискнешь?");
    }

    private String getUserStats(String userName, HeroInfo info) {
        return new StringJoiner(", ").add(userName + ", Класс: " + info.getType().getLabel())
                .add("Здоровье: " + info.getDamageGot().getStatus() + (info.getDamageGot().getValue() > HeroDamage.MEDIUM.getValue() ? (info.getDamageGot() == DEAD ? "(го завтра?)" : " (опасно)") : " (го в данж)"))
                .add("Защита: " + info.getShield())
                .add("Уровень: " + info.getLevel())
                .add("Опыт: " + info.getExperience())
                .add((CollectionUtils.isEmpty(info.getArtifacts()) ? "Нет артефактов" : ("Артефакты: " + info.getArtifacts()
                        .stream()
                        .map(Artifact::getName)
                        .collect(Collectors.joining(", ")))))
                .toString();
    }

    private String getUserShortStats(String userName, HeroInfo info) {
        return userName + " " + info.getType().getLabel();
    }

    private FightResult fight(String name, HeroInfo heroInfo) {
        BossInfo boss = getCurrentOrNext();
        if (boss == null) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        FightResult result = new FightResult()
                .setBoss(boss)
                .setHero(heroInfo);


        // hero
        HeroDamage bossDamage = null;
        boolean shield = false;
        if (heroInfo.getShield() > 0) {
            bossDamage = SHIELD;
            shield = true;
        }

        int crit = 0;
        if (bossDamage == null) {
            bossDamage = NONE;
            while (bossDamage.getValue() <= 0) {
                crit++;
                bossDamage = boss.getStrong() == heroInfo.getType() ? HeroDamage.BIG : HeroDamage.getByValue(Randomizer.getPercent() % HeroDamage.BIG.getValue());
            }
        }

        // boss
        int heroDamage = heroInfo.getAttack(boss) * Math.max(crit, 1);
        boss.dealDamage(heroDamage);
        result.setDamageDone(heroDamage);
        result.setCrit(crit);
        bossService.damage(heroInfo.getName(), heroDamage);

        result.setDamageReceived(bossDamage);

        HeroDamage join = heroInfo.getDamageGot() == null || bossDamage.getValue() < 0 ? heroInfo.getDamageGot() : heroInfo.getDamageGot().join(bossDamage);
        boolean finalShield = shield;
        Consumer<HeroInfo> update = hero -> {
            // todo remove name + start damage update after some time
            hero.setName(name);
            if (hero.getDamageGot() == null) {
                hero.setDamageGot(HeroDamage.NONE);
            }
            if (join == DEAD) {
                hero.setDeadTime(now);
            }
            hero.setDamageGot(join);
            if (finalShield) {
                hero.setShield(0);
            }
        };
        update.accept(heroInfo);
        earnXP(result);

        heroInfoService.update(name, heroInfo1 -> {
            update.accept(heroInfo1);
            heroInfo1.setExperience(heroInfo.getExperience());
        });

        return result;
    }

    private BossInfo getCurrentOrNext() {
        return bossService.getCurrentBoss();
    }

    public void earnXP(FightResult fight) {
        fight.setExp((fight.getDamageReceived().getValue() * 10) + fight.getDamageDone() + fight.getBoss().getStage() + Randomizer.nextInt(50));
        fight.getHero().setExperience(fight.getHero().getExperience() + fight.getExp());
    }

}
