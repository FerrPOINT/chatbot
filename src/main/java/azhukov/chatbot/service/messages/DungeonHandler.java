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
//            if (lowerCase.contains("!пати")) {
//
//            }
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
                dungeonService.resetCurrAcc(message.getUserName());
                return createUserMessage(message, "резет {DOGGIE}");
            }
            if (lowerCase.contains("!боссрезет13")) {
                dungeonService.resetCurrBoss();
                return createUserMessage(message, "резет {DOGGIE}");
            }
            if (lowerCase.contains("!призма13")) {
                heroInfoService.addArtifact(message.getUserName(), "prism");
                heroInfoService.addArtifact(message.getUserName(), "ozara-item");
                return createUserMessage(message, "получи призму) {DOGGIE}");
            }
            if (lowerCase.contains("!дистинкт13")) {
                heroInfoService.distinctAllArtifacts();
                return createUserMessage(message, "дистинкт {DOGGIE}");
            }
            if (lowerCase.contains("!апревардс13")) {
                dungeonService.updateRewards();
                return createUserMessage(message, "ревардс {DOGGIE}");
            }
            if (lowerCase.contains("!мигра113")) {
                dungeonService.migra1();
                return createUserMessage(message, "ревардс {DOGGIE}");
            }
            if (lowerCase.contains("!стата")) {
                return createMessage(message, dungeonService.getHeroStats(message));
            }
            if (lowerCase.contains("!артефакты") || lowerCase.contains("!артифакты")) {
                return createMessage(message, dungeonService.getArtifactsMessage(message));
            }
            for (String command : COMMANDS) {
                if (lowerCase.contains(command)) {
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
