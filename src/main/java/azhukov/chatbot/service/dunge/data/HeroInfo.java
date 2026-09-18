package azhukov.chatbot.service.dunge.data;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@NoArgsConstructor
@Data
@Accessors(chain = true)
public class HeroInfo {
    private String name;
    private HeroClass type;
    private long experience;

    /** Legacy production shape. Cleared by the idempotent dungeon migration. */
    private List<Artifact> artifacts;
    private List<OwnedArtifact> ownedArtifacts = new ArrayList<>();
    private List<StolenArtifact> stolenArtifacts = new ArrayList<>();

    private HeroDamage damageGot = HeroDamage.NONE;
    private LocalDateTime deadTime;
    private int shield;
    private float crit;
    private int events;
    private boolean specialAbilityUsed;
    private int rebornPercentage;

    private long coins;
    private long coinsEarnedToday;
    private boolean healUsedToday;
    private boolean rushUsedToday;
    private long bossDamage;
    private long bossDonations;
    private Set<String> appliedOperationIds = new HashSet<>();

    public long getLevel() {
        return Math.max(1L, experience / 1000L + 1L);
    }

    public void addShields(int shields) {
        long updated = (long) getShield() + shields;
        setShield((int) Math.max(0L, Math.min(Integer.MAX_VALUE, updated)));
    }

    public void heal(int levels) {
        HeroDamage current = damageGot == null ? HeroDamage.NONE : damageGot;
        setDamageGot(HeroDamage.getByValue(current.getValue() - Math.max(0, levels)));
    }

    public void addExp(long exp) {
        if (exp >= 0) experience = experience > Long.MAX_VALUE - exp ? Long.MAX_VALUE : experience + exp;
        else experience = exp == Long.MIN_VALUE || experience < -exp ? 0L : experience + exp;
    }

    public boolean isDead() {
        return damageGot == HeroDamage.DEAD || deadTime != null;
    }

    public boolean hasArtifacts() {
        return ownedArtifacts != null && !ownedArtifacts.isEmpty();
    }

    public boolean hasArtifact(String id) {
        return id != null && hasArtifacts() && ownedArtifacts.stream().anyMatch(a -> Objects.equals(a.getId(), id));
    }

    public boolean hasStolenArtifact(String id) {
        return id != null && stolenArtifacts != null && stolenArtifacts.stream().anyMatch(a -> Objects.equals(a.getId(), id));
    }

    public OwnedArtifact findArtifact(String id) {
        return ownedArtifacts == null ? null : ownedArtifacts.stream()
                .filter(a -> Objects.equals(a.getId(), id)).findFirst().orElse(null);
    }

    public void addOwnedArtifact(OwnedArtifact artifact) {
        if (artifact == null || artifact.getId() == null || hasArtifact(artifact.getId()) || hasStolenArtifact(artifact.getId())) return;
        if (ownedArtifacts == null) ownedArtifacts = new ArrayList<>();
        artifact.normalize();
        ownedArtifacts.add(artifact);
    }

    /** Compatibility for non-dungeon reward call sites; only ownership is persisted. */
    public void addArtifact(Artifact artifact) {
        if (artifact != null) addOwnedArtifact(new OwnedArtifact(artifact.getId(), 1));
    }

    public OwnedArtifact removeOwnedArtifact(String id) {
        OwnedArtifact artifact = findArtifact(id);
        if (artifact != null) ownedArtifacts.remove(artifact);
        return artifact;
    }

    public List<OwnedArtifact> safeOwnedArtifacts() {
        if (ownedArtifacts == null) ownedArtifacts = new ArrayList<>();
        return ownedArtifacts;
    }

    public List<StolenArtifact> safeStolenArtifacts() {
        if (stolenArtifacts == null) stolenArtifacts = new ArrayList<>();
        return stolenArtifacts;
    }

    public Set<String> safeAppliedOperationIds() {
        if (appliedOperationIds == null) appliedOperationIds = new HashSet<>();
        return appliedOperationIds;
    }
}
