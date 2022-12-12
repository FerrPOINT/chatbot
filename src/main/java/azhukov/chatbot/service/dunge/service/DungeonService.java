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

    public synchronized String getDungeonResponse1(ChatRequest request) {
        String userName = request.getUserName();
        HeroInfo current = heroInfoService.getCurrent(userName);
        if (current == null) {
            current = heroInfoService.createNew(userName);
            return "Новый герой - " + getUserShortStats(userName, current) + " уже готов спуститься в подземелье, подумайте, может не надо? {DOGGIE}";
        }
        if (current.getDamageGot() == DEAD) {
            return "Вы мертвы, ждите респауна {DOGGIE}";
        }
        if (current.getDeadTime() != null) {
            return "Вы мертвы, ваша душа блуждает в другом мире, подождите чтобы вновь обрести физическую оболочку! {DOGGIE}";
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
        return "вы нанесли " + fight.getDamageDone() + " урона " + boss.getName() + ", " + (boss.isDead() ? "вы завалили босса, поздравляем!" : "у него осталось " + boss.getCurrentHp() + " ХП");
    }

    private String getHeroMessage(FightResult fight) {
        BossInfo boss = fight.getBoss();
        HeroInfo hero = fight.getHero();
        HeroDamage damageGet = fight.getDamageGet();
        return (damageGet == HeroDamage.NONE ? "Вам удалось увернуться от всех атак " + boss.getName() : "Вам досталось от " + boss.getName() + " " + damageGet.getLabel() + ", вместе с этим вы получили " + hero.getDamageGot().getLabel());
    }

    private String getUserStats(String userName, HeroInfo info) {
        return new StringJoiner(", ").add(userName + " " + info.getType().getLabel()).add("здоровье: " + info.getDamageGot().getLabel()).add("уровень: " + info.getLevel()).add("опыт: " + info.getExperience()).add((CollectionUtils.isEmpty(info.getArtifacts()) ? "нет артефактов" : ("артефакты: " + info.getArtifacts().stream().map(Artifact::getName).collect(Collectors.joining(","))))).toString();
    }

    private String getUserShortStats(String userName, HeroInfo info) {
        return userName + " " + info.getType().getLabel();
    }

    private FightResult fight(String name, HeroInfo heroInfo) {
        BossInfo boss = getCurrentOrNext();
        if (boss == null) {
            return null;
        }
        FightResult result = new FightResult().setBoss(boss).setHero(heroInfo);

        // boss
        int heroDamage = heroInfo.getAttack();
        boss.dealDamage(heroDamage);
        result.setDamageDone(heroDamage);
        bossService.damage(heroInfo.getName(), heroDamage);

        // hero
        HeroDamage bossDamage = HeroDamage.getByPercent(Randomizer.getPercent());
        result.setDamageGet(bossDamage);
        HeroDamage join = heroInfo.getDamageGot() == null ? bossDamage : heroInfo.getDamageGot().join(bossDamage);
        Consumer<HeroInfo> update = hero -> {
            // todo remove name + start damage update after some time
            hero.setName(name);
            if (hero.getDamageGot() == null) {
                hero.setDamageGot(HeroDamage.NONE);
            }
            if (join == DEAD) {
                hero.setDeadTime(LocalDateTime.now());
            }
            hero.setDamageGot(join);

        };
        update.accept(heroInfo);
        heroInfoService.update(name, update);

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

}
