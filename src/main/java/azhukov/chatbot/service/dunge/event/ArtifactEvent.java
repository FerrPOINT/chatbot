package azhukov.chatbot.service.dunge.event;

import azhukov.chatbot.service.dunge.ArticfactService;
import azhukov.chatbot.service.dunge.data.Artifact;
import azhukov.chatbot.service.dunge.data.HeroInfo;
import azhukov.chatbot.service.dunge.service.BossService;
import azhukov.chatbot.service.util.Randomizer;
import azhukov.chatbot.service.weight.Weight;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Set;

@RequiredArgsConstructor
@Component
public class ArtifactEvent implements DungeEvent {

    private static final int XP_BY_ART = 500;

    private final BossService bossService;
    private final ArticfactService articfactService;

    @Override
    public String handle(HeroInfo hero) {
        Set<String> oldRewards = bossService.getOldRewards();
        String randomItem = Randomizer.getRandomItem(new ArrayList<>(oldRewards));
        Artifact randomArt = articfactService.getById(randomItem);
        boolean added = hero.getArtifacts() == null || hero.getArtifacts().stream().noneMatch(artifact -> artifact.getId().equals(randomItem));
        if (added) {
            hero.addArtifact(randomArt);
        } else {
            hero.setExperience(hero.getExperience() + XP_BY_ART);
        }
        return "тайник. Кто-то из предыдущих героев оставил заначку в виде артефакта: " + randomArt.getName() +
                (added ? ". Вы спешно прибераете находку к своим лапам!" : (". У вас уже есть такой, по этому вы жертвуете артефакт на алтаре Догена и получаете " + XP_BY_ART + " опыта."));
    }

    @Override
    public Weight getWeight() {
        return Weight.HIGH;
    }

}
