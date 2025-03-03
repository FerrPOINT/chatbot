package azhukov.chatbot.service.messages;

import azhukov.chatbot.dto.ChatRequest;
import azhukov.chatbot.dto.ChatResponse;
import azhukov.chatbot.service.dunge.ability.HeroAbilityService;
import azhukov.chatbot.service.dunge.service.BossService;
import azhukov.chatbot.service.dunge.service.DungeonService;
import azhukov.chatbot.service.dunge.service.HeroInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DungeonHandler extends MessageHandler {

    private final DungeonService dungeonService;
    private final BossService bossService;
    private final HeroInfoService heroInfoService;
    private final HeroAbilityService heroAbilityService;

    private static final List<String> COMMANDS = List.of("!данж", "!dungeon");
    private static final List<String> BOSS_COMMANDS = List.of("!босс", "!boss");
    private static final List<String> STAT_COMMANDS = List.of("!стата", "!статус");
    private static final List<String> ARTS_COMMANDS = List.of("!артефакты", "!артифакты");
    private static final List<String> ABILITY_COMMANDS = List.of("!абилка", "!способность", "!ульт", "!абилити");
    private static final List<String> INFO_COMMANDS = List.of("!инфо");
    private static final List<String> COMMANDS_COMMANDS = List.of("!команды");

    @Override
    public ChatResponse answerMessage(ChatRequest message, String text, String lowerCase) {
        if (lowerCase != null) {
//            if (lowerCase.contains("!superuser13")) {
//                dungeonService.superuser(message);
//                return createMessage(message, "OK");
//            }
            for (String command : COMMANDS_COMMANDS) {
                if (lowerCase.contains(command)) {
                    return createMessage(message,
                            "Команды: " + String.join(" | ", INFO_COMMANDS.toString(), COMMANDS.toString(), BOSS_COMMANDS.toString(), STAT_COMMANDS.toString(), ARTS_COMMANDS.toString(), ABILITY_COMMANDS.toString(), "!герои", "!ладдер")
                    );
                }
            }
            if (lowerCase.contains("!герои")) {
                return createMessage(message, dungeonService.getHeroesListResponse());
            }
            if (lowerCase.contains("!ладдер")) {
                return createMessage(message, dungeonService.getLadderResponse());
            }
            for (String command : ABILITY_COMMANDS) {
                if (lowerCase.contains(command)) {
                    return createMessage(message, dungeonService.useHeroAbility(message));
                }
            }
            for (String command : INFO_COMMANDS) {
                if (lowerCase.contains(command)) {
                    return createMessage(message, dungeonService.getHeroInfo(message));
                }
            }
            for (String command : STAT_COMMANDS) {
                if (lowerCase.contains(command)) {
                    return createMessage(message, dungeonService.getHeroStats(message));
                }
            }
            for (String command : ARTS_COMMANDS) {
                if (lowerCase.contains(command)) {
                    return createMessage(message, dungeonService.getArtifactsMessage(message));
                }
            }
            for (String command : COMMANDS) {
                if (lowerCase.contains(command)) {
//                    return createUserMessage(message, "На данже висит табличка \"Dungeons and Doggies 2 coming soon\" {DOGGIE}");
                    return createMessage(message, dungeonService.getDungeonResponse(message));
                }
            }
            for (String command : BOSS_COMMANDS) {
                if (lowerCase.contains(command)) {
                    return createUserMessage(message, bossService.getCurrentBossData());
                }
            }
        }
        return null;
    }
}
