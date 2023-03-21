package azhukov.chatbot.service.messages;

import azhukov.chatbot.dto.ChatRequest;
import azhukov.chatbot.dto.ChatResponse;
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

    private static final List<String> COMMANDS = List.of("!данж", "!dungeon", "!данжн");
    private static final List<String> BOSS_COMMANDS = List.of("!босс", "!boss");

    @Override
    public ChatResponse answerMessage(ChatRequest message, String text, String lowerCase) {
        if (lowerCase != null) {
            if (lowerCase.contains("!резетвсё13")) {
                dungeonService.resetAccs();
                dungeonService.resetBoss();
                return createUserMessage(message, "резет {DOGGIE}");
            }
            if (lowerCase.contains("!резетбосс13")) {
                dungeonService.resetBoss();
                return createUserMessage(message, "резет {DOGGIE}");
            }
            if (lowerCase.contains("!резетаккс13")) {
                dungeonService.resetAccs();
                return createUserMessage(message, "резет {DOGGIE}");
            }
            if (lowerCase.contains("!резеткур13")) {
                dungeonService.resetCurrAcc();
                return createUserMessage(message, "резет {DOGGIE}");
            }
            if (lowerCase.contains("!призма13")) {
                heroInfoService.addArtifact(message.getUserName(), "prism");
                return createUserMessage(message, "получи призму) {DOGGIE}");
            }
            if (lowerCase.contains("!стата")) {
                return createUserMessage(message, dungeonService.getHeroStats(message));
            }
            for (String command : COMMANDS) {
                if (lowerCase.contains(command)) {
                    return createUserMessage(message, dungeonService.getDungeonResponse(message));
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
