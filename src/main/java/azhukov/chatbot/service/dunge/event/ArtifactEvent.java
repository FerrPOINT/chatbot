package azhukov.chatbot.service.dunge.event;

import azhukov.chatbot.service.dunge.ArtifactCatalog;
import azhukov.chatbot.service.dunge.DungeonRandom;
import azhukov.chatbot.service.dunge.data.Artifact;
import azhukov.chatbot.service.dunge.data.HeroInfo;
import azhukov.chatbot.service.dunge.data.OwnedArtifact;
import azhukov.chatbot.service.dunge.service.BossService;
import azhukov.chatbot.service.weight.Weight;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ArtifactEvent implements DungeEvent {
    private static final long XP_BY_ART = 1000;
    private final BossService bosses;
    private final ArtifactCatalog artifacts;
    private final DungeonRandom random;

    @Override
    public String handle(HeroInfo hero) {
        List<String> candidates = bosses.getOldRewards().stream()
                .filter(id -> artifacts.get(id) != null && !hero.hasArtifact(id) && !hero.hasStolenArtifact(id)).toList();
        if (candidates.isEmpty()) {
            hero.addExp(XP_BY_ART);
            return "тайник без новой реликвии; алтарь даёт " + XP_BY_ART + " опыта";
        }
        String id = random.item(candidates);
        Artifact definition = artifacts.get(id);
        hero.addOwnedArtifact(new OwnedArtifact(id, 1));
        return "тайник с артефактом «" + definition.getName() + "». Получен базовый ранг 1";
    }

    @Override public Weight getWeight() { return Weight.HIGHEST; }
}
