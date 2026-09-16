package azhukov.chatbot.service.dunge.event;

import azhukov.chatbot.service.dunge.ArtifactCatalog;
import azhukov.chatbot.service.dunge.DungeonRandom;
import azhukov.chatbot.service.dunge.data.HeroInfo;
import azhukov.chatbot.service.dunge.data.StolenArtifact;
import azhukov.chatbot.service.dunge.service.DungeonEconomyService;
import azhukov.chatbot.service.weight.Weight;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StealEvent implements DungeEvent {
    private final DungeonEconomyService economy;
    private final DungeonRandom random;
    private final ArtifactCatalog artifacts;

    @Override
    public String handle(HeroInfo hero) {
        StolenArtifact stolen = economy.stealRandom(hero, 15, "DOGGIE_ROGUE_EVENT", random);
        if (stolen == null) hero.addExp(500);
        String name = stolen == null || artifacts.get(stolen.getId()) == null ? null : artifacts.get(stolen.getId()).getName();
        return "святилище Догги-Роги. " + (stolen != null
                ? "Опасность унесла артефакт: " + name + ". Его можно вернуть через !выкуп " + stolen.getId()
                : "Молитва удалась: получено 500 опыта");
    }

    @Override public Weight getWeight() { return Weight.RARE; }
}
