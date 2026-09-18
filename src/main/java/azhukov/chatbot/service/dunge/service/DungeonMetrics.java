package azhukov.chatbot.service.dunge.service;

import lombok.experimental.UtilityClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@UtilityClass
public class DungeonMetrics {
    private final Logger LOG = LoggerFactory.getLogger("dungeon.metrics");

    public void operation(String status, String id, String type, String boss, String hero) {
        LOG.info("event=operation status={} operation_id={} type={} boss_instance={} hero_hash={}", status, id, type, boss, hash(hero));
    }

    public void damage(String source, String boss, long before, long after, long real, String hero) {
        LOG.info("event=boss_damage source={} boss_instance={} hp_before={} hp_after={} real_damage={} hero_hash={}",
                source, boss, before, after, real, hash(hero));
    }

    public void death(String hero, int penaltyPercent, long experience, long coins, Object reviveAt) {
        LOG.info("event=hero_death hero_hash={} penalty_percent={} experience_after={} coins_after={} revive_at={}",
                hash(hero), penaltyPercent, experience, coins, reviveAt);
    }

    public void economy(String action, String hero, long coins, long amount) {
        LOG.info("event=economy action={} hero_hash={} coins_after={} amount={}", action, hash(hero), coins, amount);
    }

    public void coinIncome(String hero, long coins, long earnedToday, long cap, long amount) {
        LOG.info("event=economy action=earn hero_hash={} coins_after={} earned_today={} daily_cap={} amount={}",
                hash(hero), coins, earnedToday, cap, amount);
    }

    public void artifact(String action, String hero, String artifactId, int rank, String source) {
        LOG.info("event=artifact action={} hero_hash={} artifact_id={} rank={} source={}",
                action, hash(hero), artifactId, rank, source);
    }

    public void buff(String action, String type, int value, String operationId) {
        LOG.info("event=global_buff action={} buff_type={} value={} operation_id={}", action, type, value, operationId);
    }

    public String hash(String value) {
        if (value == null) return "none";
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 8);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
