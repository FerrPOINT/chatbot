package azhukov.chatbot.service.dunge.service;

import azhukov.chatbot.dto.ChatRequest;
import azhukov.chatbot.service.combination.CombinationService;
import azhukov.chatbot.service.dunge.ArticfactService;
import azhukov.chatbot.service.dunge.ability.AbilitiesData;
import azhukov.chatbot.service.dunge.ability.HeroAbilityService;
import azhukov.chatbot.service.dunge.data.*;
import azhukov.chatbot.service.dunge.event.DungeEvent;
import azhukov.chatbot.service.messages.MessageContainer;
import azhukov.chatbot.service.store.DailyStore;
import azhukov.chatbot.service.store.Store;
import azhukov.chatbot.service.util.DistinctEvictingCollection;
import azhukov.chatbot.service.util.Randomizer;
import azhukov.chatbot.service.weight.WeightUtils;
import azhukov.chatbot.util.RangesContainer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static azhukov.chatbot.service.dunge.data.HeroDamage.*;

@Service
@RequiredArgsConstructor
public class DungeonService {

    private static final int MAX_EVENTS_PER_DAY = 3;

    private final HeroInfoService heroInfoService;
    private final BossService bossService;
    private final ArticfactService articfactService;
    private final DailyStore dailyStore;
    private final List<DungeEvent> events;
    private final CombinationService combinationService;
    private final HeroAbilityService heroAbilityService;
    private RangesContainer<DungeEvent> eventsContainer;

    private final DistinctEvictingCollection<String> previousHero = new DistinctEvictingCollection<>(10); // Переменная для хранения предыдущего героя

    @PostConstruct
    void init() {
        this.eventsContainer = WeightUtils.createContainer(events);
    }

    //every 10 mins
    @Scheduled(cron = "0 */10 * ? * *")
    void healScheduled() {
        Store dailySettings = dailyStore.getDailySettings();
        String heal = dailySettings.get("DAILY_HEAL");
        if (heal == null) {
            heroInfoService.healAll();
            bossService.updateCurrentBoss(bossInfo -> {
                if (!bossInfo.isDead()) {
                    int threshold = bossInfo.getMaxHp() / 2 - bossInfo.getDamageReceived();
                    bossInfo.heal(threshold <= 0 ? Math.abs(threshold) : 0);
                    bossInfo.resetOpponents();
                }
            });
            dailySettings.put("DAILY_HEAL", "true");
        }
    }

    public String getHeroStats(ChatRequest request) {
        String userName = request.getUserName();
        HeroInfo current = heroInfoService.getCurrent(userName);
        return current != null ? getUserStats(userName, current) : "Вас нет в мире живых, но вы уже готовы возродиться героем!";
    }

    public String getHeroInfo(ChatRequest request) {
        String userName = request.getUserName();
        HeroInfo current = heroInfoService.getCurrent(userName);
        return current != null ? getUserInfo(userName, current) : "Вас нет в мире живых, но вы уже готовы возродиться героем!";
    }

    public synchronized String getDungeonResponse(ChatRequest request) {
        String userName = request.getUserName();
        HeroInfo current = heroInfoService.getCurrent(userName);
        if (current == null) {
            current = heroInfoService.createNew(userName);
            return "Новый герой - " + getUserShortStats(userName, current) + " уже готов спуститься в подземелье !данж !стата !босс !артефакты !ладдер !герои {DOGGIE}";
        }
        if (current.getDamageGot() == DEAD || current.getDeadTime() != null) {
            return "Вы мертвы, ваша душа блуждает в другом мире, подождите чтобы вновь обрести физическую форму! {DOGGIE}";
        }
        BossInfo boss = getCurrentOrNext();
        if (boss == null || boss.isDead()) {
            return "Данж разорён, тут совсем пусто, подождём пока тут поселится новый злодей! {DOGGIE}";
        }
        try {
            if (current.getEvents() == 0 || (current.getEvents() < MAX_EVENTS_PER_DAY && Randomizer.nextInt(1, 11) < (5 - current.getEvents()))) {
                DungeEvent randomEvent = eventsContainer.getRandomItem();
                MessageContainer message = new MessageContainer();
                heroInfoService.update(request.getUserName(), heroInfo -> {
                    message.setMessage(randomEvent.handle(heroInfo) + " {DOGGIE}");
                    heroInfo.setEvents(heroInfo.getEvents() + 1);
                });
                return combinationService.getRandomCombinationMessage("dunge-prefix") + " " + message.getMessage();
            }
            FightResult fight = fight(userName, current);
            try {
                return getBossMessage(fight) + ". " + getHeroMessage(fight) + getRewardsMessage(fight) + " {DOGGIE}";
            } finally {
                heroAbilityService.resetNextHeroAbilities();
            }
        } finally {
            previousHero.add(current.getName());
        }
    }

    public String useHeroAbility(ChatRequest message) {
        HeroInfo curr = heroInfoService.getCurrent(message.getUserName());
        if (curr == null) {
            return "Герой не найден.";
        }
        String prevName = previousHero.getPrev();
        HeroInfo prev = prevName == null ? null : heroInfoService.getCurrent(prevName);
        return heroAbilityService.useHeroAbility(curr, prev);
    }

    private String getRewardsMessage(FightResult fight) {
        if (fight.getBoss().isDead() && CollectionUtils.isNotEmpty(fight.getBoss().getRewards())) {
            List<Artifact> rewards = fight.getBoss().getRewards().stream().map(articfactService::getById).filter(Objects::nonNull).toList();
            if (!rewards.isEmpty()) {
                for (String damagedHero : fight.getBoss().getDamagedHeroes()) {
                    heroInfoService.update(damagedHero, hi -> {
                        for (Artifact reward : rewards) {
                            hi.addArtifact(reward);
                        }
                    });
                }
                heroInfoService.update(fight.getHero().getName(), hi -> {
                    for (Artifact reward : rewards) {
                        hi.addArtifact(reward);
                    }
                });
                return ". Все кто участвовал и выжил получают " + rewards.stream().map(Artifact::getName).collect(Collectors.joining(", "));
            }
        }
        return "";
    }

    private String getBossMessage(FightResult fight) {
        BossInfo boss = fight.getBoss();
        HeroInfo hero = fight.getHero();
        return "Данж " + hero.getName() + " " + fight.getHero().getType().getLabel() + " VS " + boss.getName() + " " + fight.getBoss().getLabel() +
                ". Продолжительность боя: " + fight.getFightsNumber() + " раундов." + (fight.isImmunity() ? " У босса иммунитет против вашего класса" :
                (" Ваш урон " + fight.getDamageDone() + (fight.getCrit() > 1 ? ", КРИТ Х" + fight.getCrit() : ""))) +
                (boss.isDead() ? (", ВЫ ЗАВАЛИЛИ БОССА, ПОЗДРАВЛЯЕМ! Все бившиеся герои получают: " + getRewardsString(boss)) : (fight.isImmunity() ? "" : ", у босса осталось " + boss.getCurrentHp() + " HP"));
    }

    private String getRewardsString(BossInfo boss) {
        Set<String> rewards = boss.getRewards();
        if (CollectionUtils.isEmpty(rewards)) {
            return "ничего))";
        }
        return rewards.stream().map(articfactService::getById).map(artifact -> artifact.getName() + " - " + artifact.getLabel()).collect(Collectors.joining(", "));
    }

    private String getHeroMessage(FightResult fight) {
        BossInfo boss = fight.getBoss();
        HeroInfo hero = fight.getHero();
        HeroDamage damageGet = fight.getDamageReceived();
        if (hero.getDamageGot() == DEAD) {
            return "Вы рискнули напасть на этого матёрого босса, и он вам нанес " + damageGet.getLabel() + ", вместе с этим вы получили смертельный урон, PRESS F!";
        }
        return (fight.isReborn() ? "Босс уже добивал вас, но вы спаслись с помощью абилки избегания СМЕРТИ. " : "Вы сбежали из-за опасности. ") + (damageGet.getValue() <= 0 ? "Вы всё задоджили " :
                "Вам досталось " + damageGet.getLabel()) +
                (fight.getStealArt() == null ? "" : ", в бою у вас УКРАЛИ артефакт: " + fight.getStealArt()) +
                (fight.getShieldSpent() > 0 ? ", потрачено брони: " + fight.getShieldSpent() : "") +
                ", получено опыта: " + fight.getExp() + ", общий статус: " + hero.getDamageGot().getStatus() + ", " +
                getDangerByDamage(boss, hero);
    }

    private String getDangerByDamage(BossInfo boss, HeroInfo hero) {
        int currentDamage = hero.getDamageGot().getValue() - hero.getShield();

        if (currentDamage < BIG.getValue()) {
            return "не опасно";
        }

        if (boss != null && boss.getStrong() == hero.getType()) {
            currentDamage++;
        }

        HeroDamage byValue = getByValue(currentDamage);
        return switch (byValue) {
            case BIG -> "есть опасность";
            case HUGE -> "средняя опасность";
            case ALMOUST_DEAD -> "высокая опасность";
            case DEAD -> "тебе точно кранты";
            default -> "не опасно";
        } + ", рискнешь?";
    }

    private String getUserStats(String userName, HeroInfo info) {
        return new StringJoiner(", ")
                .add(userName + ", Класс: " + info.getType().getLabel())
                .add("Абилка: " + (info.isSpecialAbilityUsed() ? "использовалалась" : "не использовалалась"))
                .add("Здоровье: " + info.getDamageGot().getStatus() + " " + (info.getDamageGot().getValue() - info.getShield() > HeroDamage.MEDIUM.getValue() ? (info.getDamageGot() == DEAD ? "(го завтра?)" : getDangerByDamage(bossService.getCurrentBoss(), info)) : "(го в данж)"))
                .add("Защита: " + info.getShield())
                .add("Уровень: " + info.getLevel())
                .add("Опыт: " + info.getExperience())
                .add(CollectionUtils.isEmpty(info.getArtifacts()) ? "Нет артефактов" : ("Артефакты: " + info.getArtifacts().size() + " штук"))
                .toString();
    }

    private String getUserInfo(String userName, HeroInfo info) {
        return new StringJoiner(", ")
                .add(userName + ", Класс: " + info.getType().getLabel())
                .add("Абилка: " + info.getType().getAbilityName() + " - " + info.getType().getAbilityDescription())
                .toString();
    }

    public String getArtifactsMessage(ChatRequest message) {
        HeroInfo info = heroInfoService.getCurrent(message.getUserName());
        if (info == null) {
            return "А ты вобще жив?";
        }
        return info.getArtifacts() == null ? "У вас нет артефактов" : info.getArtifacts().stream().map(Artifact::getName).collect(Collectors.joining(", ", "Ваши артефакты: ", ""));
    }

    private String getUserShortStats(String userName, HeroInfo info) {
        return userName + " " + info.getType().getLabel();
    }

    private FightResult fight(String name, HeroInfo heroInfo) {
        BossInfo boss = getCurrentOrNext();

        AbilitiesData nextHeroBuffs = heroAbilityService.getNextHeroBuffs();

        LocalDateTime now = LocalDateTime.now();
        FightResult result = new FightResult()
                .setBoss(boss)
                .setHero(heroInfo)
                .setNextHeroBuffs(nextHeroBuffs);

        boolean immunity = boss.getImmunity() == heroInfo.getType();
        result.setImmunity(immunity);

        // hero
        HeroDamage damageFromBoss = NONE;
        int fights = 0;
        MutableInt crit = new MutableInt(heroInfo.getCrit() + 1);

        boolean bossStronger = boss.getStrong() == heroInfo.getType();

        int shieldEvailable = heroInfo.getShield() + nextHeroBuffs.getShieldUpdate();
        int shieldSpent = 0;

        HeroDamage damageBoundExclusive = HUGE;
        if ((heroInfo.getDamageGot().getValue() - shieldEvailable) > MEDIUM.getValue()) {
            fights++;
            damageFromBoss = getByValue(Randomizer.nextInt(bossStronger ? 1 : 0, damageBoundExclusive.getValue()));
            if (damageFromBoss != NONE && shieldEvailable > 0) {
                damageFromBoss = damageFromBoss.heal(HeroDamage.getByValue(shieldEvailable));
                shieldSpent = shieldEvailable;
                shieldEvailable = 0;
            }
            crit.increment();
        } else {
            while ((heroInfo.getDamageGot().getValue() + damageFromBoss.getValue() - shieldEvailable) < BIG.getValue()) {
                fights++;
                HeroDamage iterationDamage = getByValue(Randomizer.nextInt(bossStronger ? 2 : 1, damageBoundExclusive.getValue()));
                int damageValue = iterationDamage.getValue();
                if (shieldEvailable != 0) {
                    if (damageValue >= shieldEvailable) {
                        iterationDamage = HeroDamage.getByValue(damageValue - shieldEvailable);
                        shieldSpent += shieldEvailable;
                        shieldEvailable = 0;
                    } else {
                        iterationDamage = NONE;
                        shieldSpent += damageValue;
                        shieldEvailable = shieldEvailable - damageValue;
                    }
                }
                damageFromBoss = damageFromBoss.join(iterationDamage);
            }
        }

        result.setStealArt(tryToSteal(boss.getStealPercent(), heroInfo));

        int heroDamage = 0;
        // boss
        if (!immunity) {
            heroDamage = IntStream.range(0, fights).map(operand -> heroInfo.getAttack(boss) * crit.intValue()).sum();
            if (nextHeroBuffs.getAttackUpdate() > 0) {
                heroDamage = heroDamage + (int) ((double) heroDamage * (double) nextHeroBuffs.getAttackUpdate() / 100D);
            }
            boss.dealDamage(heroDamage);
            result.setDamageDone(heroDamage);
            result.setCrit(crit.intValue());
        }
        bossService.damage(heroInfo.getName(), heroDamage);

        result.setFightsNumber(fights);
        result.setDamageReceived(damageFromBoss);
        result.setShieldSpent(shieldSpent);

        HeroDamage join = heroInfo.getDamageGot() == null || damageFromBoss.getValue() < 0 ? heroInfo.getDamageGot() : heroInfo.getDamageGot().join(damageFromBoss);

        if (join == DEAD && heroInfo.getRebornPercentage() > 0) {
            if (Randomizer.getPercent() < heroInfo.getRebornPercentage()) {
                result.setReborn(true);
                heroInfo.setRebornPercentage(0);
                join = ALMOUST_DEAD;
            }
        }

        int finalShieldSpent = shieldSpent;
        int finalShieldEvailable = shieldEvailable;
        HeroDamage finalDamage = join;
        Consumer<HeroInfo> update = hero -> {
            if (finalDamage == DEAD) {
                hero.setDeadTime(now);
            }
            hero.setDamageGot(finalDamage);
            if (finalShieldSpent > 0) {
                hero.setShield(finalShieldEvailable);
            }
            hero.setArtifacts(heroInfo.getArtifacts());
        };
        update.accept(heroInfo);
        earnXP(result);

        heroInfoService.update(name, info -> {
            update.accept(info);
            info.setExperience(heroInfo.getExperience());
        });

        return result;
    }

    public static String tryToSteal(int stealPercent, HeroInfo heroInfo) {
        if (stealPercent > 0 && CollectionUtils.isNotEmpty(heroInfo.getArtifacts())) {
            int random = Randomizer.getPercent();
            if (random > 0 && random < stealPercent) {
                List<Artifact> artifacts = heroInfo.getArtifacts();
                if (CollectionUtils.isNotEmpty(artifacts)) {
                    Artifact randomItem = Randomizer.getRandomItem(artifacts);
                    artifacts.remove(randomItem);
                    return randomItem.getName();
                }
            }
        }
        return null;
    }

    private BossInfo getCurrentOrNext() {
        return bossService.getCurrentBoss();
    }

    public void earnXP(FightResult fight) {
        int exp = IntStream.range(0, fight.getFightsNumber()).map(operand -> (fight.getDamageReceived().getValue() * 10) + (fight.getDamageDone() / fight.getHero().getLevel()) + Math.max(0, 100 - fight.getBoss().getStage() - fight.getHero().getLevel())).sum();

        if (fight.getNextHeroBuffs().getAttackUpdate() > 0) {
            exp = exp + (int) ((double) exp * (double) fight.getNextHeroBuffs().getAttackUpdate() / 100D);
        }

        fight.setExp(exp);
        fight.getHero().setExperience(fight.getHero().getExperience() + fight.getExp());
    }

    public void updateRewards() {
        bossService.handlePrevBosses(bossInfo -> {
            BossInfo boss = bossService.getBossInfo(bossInfo.getStage());
            if (CollectionUtils.isNotEmpty(boss.getRewards())) {
                List<Artifact> rewards = boss.getRewards().stream().map(articfactService::getById).filter(Objects::nonNull).toList();
                for (String hero : bossInfo.getDamagedHeroes()) {
                    heroInfoService.update(hero, heroInfo -> rewards.forEach(heroInfo::addArtifact));
                }
            }
        });
    }

    public String getLadderResponse() {
        List<Pair<String, Integer>> topLevel = heroInfoService.getTopLevel(20);
        StringJoiner sj = new StringJoiner(",  ");
        for (int i = 0; i < topLevel.size(); i++) {
            Pair<String, Integer> p = topLevel.get(i);
            sj.add((i + 1) + ". " + p.getLeft() + " " + p.getRight() + "lvl");
        }
        return "Топ выживших в подземелье - " + sj;
    }

    public String getHeroesListResponse() {
        return HeroClass.VALUES.stream()
                .map(heroClass -> "|" + heroClass.getLabel() + " - " + heroClass.getAbilityName() + "|")
                .collect(Collectors.joining("\n", "На данный момент в подземелье котируются следующие классы:\n", "\n{DOGGIE}"));
    }

    public void superuser(ChatRequest message) {
        heroInfoService.update(message.getUserName(), heroInfo -> {
            heroInfo.setExperience(50000);
            heroInfo.setArtifacts(articfactService.getItems());
            heroInfo.setRebornPercentage(100);
            heroInfo.setSpecialAbilityUsed(false);
            heroInfo.setType(HeroClass.NECRO);
        });
    }

}
