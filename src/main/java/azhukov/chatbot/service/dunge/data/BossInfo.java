package azhukov.chatbot.service.dunge.data;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static azhukov.chatbot.constants.Constants.DUNGEON_MULTIPLIER;

@Data
public class BossInfo {
    private int stage;
    private int cycle = 1;
    private int level;
    private String name;
    private String label;
    private String minionsName;
    private String minionsLabel;
    private HeroClass strong;
    private HeroClass weak;
    private long damageReceived;
    private long maxHp;
    private Set<String> damagedHeroes = new HashSet<>();
    private Set<String> rewards = new HashSet<>();
    private int stealPercent;
    private HeroClass immunity;
    private long treasury;
    private long normalDamage;
    private long abilityDamage;
    private long rushDamage;
    private long siegeDamage;
    private Map<String, Long> damageByHero = new HashMap<>();
    private Map<String, Long> donationsByHero = new HashMap<>();
    private Set<String> appliedOperationIds = new HashSet<>();
    private boolean rewardsGranted;
    private LocalDateTime startedAt;
    private boolean startedAtEstimated;

    public long getMaxHp() {
        return maxHp > 0 ? maxHp : Math.max(1L, (long) level * DUNGEON_MULTIPLIER);
    }

    @JsonIgnore
    public long getStoredMaxHp() { return maxHp; }

    @JsonIgnore
    public String getInstanceId() {
        return Math.max(1, cycle) + ":" + stage;
    }

    @JsonIgnore
    public boolean isDead() {
        return damageReceived >= getMaxHp();
    }

    public long dealDamage(long requestedDamage) {
        long realDamage = Math.min(Math.max(0L, requestedDamage), Math.max(0L, getCurrentHp()));
        damageReceived += realDamage;
        return realDamage;
    }

    @JsonIgnore
    public long getCurrentHp() {
        return Math.max(0L, getMaxHp() - damageReceived);
    }

    public Set<String> safeDamagedHeroes() {
        if (damagedHeroes == null) damagedHeroes = new HashSet<>();
        return damagedHeroes;
    }

    public Set<String> safeRewards() {
        if (rewards == null) rewards = new HashSet<>();
        return rewards;
    }

    public Map<String, Long> safeDamageByHero() {
        if (damageByHero == null) damageByHero = new HashMap<>();
        return damageByHero;
    }

    public Map<String, Long> safeDonationsByHero() {
        if (donationsByHero == null) donationsByHero = new HashMap<>();
        return donationsByHero;
    }

    public Set<String> safeAppliedOperationIds() {
        if (appliedOperationIds == null) appliedOperationIds = new HashSet<>();
        return appliedOperationIds;
    }

    public void addDamageBySource(DungeonOperation.Type type, long damage) {
        switch (type) {
            case FIGHT -> normalDamage = saturatedAdd(normalDamage, damage);
            case ABILITY_DAMAGE -> abilityDamage = saturatedAdd(abilityDamage, damage);
            case RUSH -> rushDamage = saturatedAdd(rushDamage, damage);
            case SIEGE -> siegeDamage = saturatedAdd(siegeDamage, damage);
            default -> { }
        }
    }

    private long saturatedAdd(long left, long right) {
        if (right <= 0) return left;
        return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }
}
