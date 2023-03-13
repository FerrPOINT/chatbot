package azhukov.chatbot.service.dunge.service;

import azhukov.chatbot.dto.ChatRequest;
import azhukov.chatbot.service.dunge.data.*;
import azhukov.chatbot.service.util.Randomizer;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static azhukov.chatbot.service.dunge.data.HeroDamage.DEAD;

@Service
@RequiredArgsConstructor
public class DungeonService {

    private final HeroInfoService heroInfoService;
    private final BossService bossService;

    //every day at 6:00
    @Scheduled(cron = "0 0 6 * * ?")
    void healScheduled() {
        heroInfoService.healAll();
    }

    public synchronized String getDungeonResponse(ChatRequest request) {
        String userName = request.getUserName();
        HeroInfo current = heroInfoService.getCurrent(userName);
        if (current == null) {
            current = heroInfoService.createNew(userName);
            return "Новый герой - " + getUserShortStats(userName, current) + " уже готов спуститься в подземелье, подумайте, может не надо? {DOGGIE}";
        }
        if (current.getDeadTime() != null) {
            return "Вы мертвы, ваша душа блуждает в другом мире, подождите чтобы вновь обрести физическую оболочку! {DOGGIE}";
        }
        return "Данж пока закрыт на ремонт, но вы не теряете времени и уже собираетесь в путешествие - " + getUserStats(userName, current);
    }

    public String getHeroStats(ChatRequest request) {
        String userName = request.getUserName();
        HeroInfo current = heroInfoService.getCurrent(userName);
        return getUserStats(userName, current);
    }

    public synchronized String getDungeonResponse1(ChatRequest request) {
        String userName = request.getUserName();
        HeroInfo current = heroInfoService.getCurrent(userName);
        if (current == null) {
            current = heroInfoService.createNew(userName);
            return "Новый герой - " + getUserShortStats(userName, current) + " уже готов спуститься в подземелье, подумайте, может не надо? {DOGGIE}";
        }
        if (current.getDamageGot() == DEAD || current.getDeadTime() != null) {
            return "Вы мертвы, ваша душа блуждает в другом мире, подождите чтобы вновь обрести физическую форму! {DOGGIE}";
        }
        FightResult fight = fight(userName, current);

        if (fight == null) {
            return "Данж разорён, тут совсем пусто, подождём пока тут поселится новый злодей! {DOGGIE}";
        }

        return getBossMessage(fight) + ". " + getHeroMessage(fight) + " {DOGGIE}";
    }

    public void reset() {
        heroInfoService.reset();
        bossService.reset();
    }

    private String getBossMessage(FightResult fight) {
        BossInfo boss = fight.getBoss();
        return "Вы нанесли " + fight.getDamageDone() + " урона " + boss.getName() + ", " + (boss.isDead() ? "вы завалили босса, поздравляем!" : "у него осталось " + boss.getCurrentHp() + " ХП");
    }

    private String getHeroMessage(FightResult fight) {
        BossInfo boss = fight.getBoss();
        HeroInfo hero = fight.getHero();
        HeroDamage damageGet = fight.getDamageGet();
        if (hero.getDamageGot() == DEAD) {
            return "Вы рискнули напасть на " + boss.getName() + ", но он вам нанес " + damageGet.getLabel() + ", вместе с этим вы получили смертельный урон, press F";
        }
        return (damageGet == HeroDamage.NONE ? "Вам удалось увернуться от всех атак " + boss.getName() :
                "Вам досталось от " + boss.getName() + " " + damageGet.getLabel() +
                        ", вы получили " + fight.getExp() + " опыта, общий статус: " + hero.getDamageGot().getStatus()) +
                (hero.getDamageGot().getValue() < HeroDamage.BIG.getValue() ? ""
                        : ", сегодня ходить в данж уже опасно, но вы можете рискнуть");
    }

    private String getUserStats(String userName, HeroInfo info) {
        return new StringJoiner(", ").add(userName + ", Класс: " + info.getType().getLabel())
                .add("Здоровье: " + info.getDamageGot().getStatus() + (info.getDamageGot().getValue() > HeroDamage.MEDIUM.getValue() ? " (опасно)" : " (го в данж)"))
                .add("Уровень: " + info.getLevel())
                .add("Опыт: " + info.getExperience())
                .add((CollectionUtils.isEmpty(info.getArtifacts()) ? "Нет артефактов" : ("Артефакты: " + info.getArtifacts()
                        .stream()
                        .map(Artifact::getName)
                        .collect(Collectors.joining(",")))))
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

        // boss
        int heroDamage = heroInfo.getAttack(boss);
        boss.dealDamage(heroDamage);
        result.setDamageDone(heroDamage);
        bossService.damage(heroInfo.getName(), heroDamage);

        // hero
        HeroDamage bossDamage = boss.getStrong() == heroInfo.getType() ? HeroDamage.BIG : HeroDamage.getByValue(Randomizer.getPercent() % HeroDamage.BIG.getValue());
        result.setDamageGet(bossDamage);
        HeroDamage join = heroInfo.getDamageGot() == null ? bossDamage : heroInfo.getDamageGot().join(bossDamage);
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
        };
        update.accept(heroInfo);
        earnXP(result);

        heroInfoService.update(name, heroInfo1 -> {
            update.accept(heroInfo1);
            heroInfo1.setExperience(heroInfo.getExperience());
        });

        return result;
    }

    private synchronized BossInfo getCurrentOrNext() {
        BossInfo currentBoss = bossService.getCurrentBoss();
        if (currentBoss.isDead()) {
            BossInfo next = bossService.next(currentBoss);
            if (next == null) {
                return null;
            }
            if (next == currentBoss) {
                return null;
            }
            currentBoss = next;
        }
        return currentBoss;
    }

    public void earnXP(FightResult fight) {
        fight.setExp((fight.getDamageGet().getValue() * 10) + fight.getDamageDone() + fight.getBoss().getStage() + Randomizer.nextInt(50));
        fight.getHero().setExperience(fight.getHero().getExperience() + fight.getExp());
    }

}
