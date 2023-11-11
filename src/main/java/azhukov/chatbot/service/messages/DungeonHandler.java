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
    private static final List<String> STAT_COMMANDS = List.of("!стата", "!статус");
    private static final List<String> ARTS_COMMANDS = List.of("!артефакты", "!артифакты");

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
                dungeonService.resetAcc(message.getUserName());
                return createUserMessage(message, "резет {DOGGIE}");
            }
            if (lowerCase.contains("!резурект13")) {
                dungeonService.resurrectAcc(message.getText().split(" ")[1]);
                return createUserMessage(message, "резурект {DOGGIE}");
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
            if (lowerCase.contains("!апшилд")) {
                String[] tokens = message.getText().split(" ");
                dungeonService.upShield(tokens.length == 1 ? message.getUserName() : tokens[1]);
                return createUserMessage(message, "апшилд {DOGGIE}");
            }
            if (lowerCase.contains("!данж13")) {
                return createMessage(message, dungeonService.getDungeonResponse(message));
            }
            if (lowerCase.contains("!артефакт13")) {
                heroInfoService.addArtifact(message.getText().split(" ")[1], message.getText().split(" ")[2]);
                return createUserMessage(message, "артефакт {DOGGIE}");
            }
            if (lowerCase.contains("!ладдер")) {
                return createMessage(message, dungeonService.getLadderResponse());
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
//                    return createUserMessage(message, "На данже висит табличка \"Закрыто на ремонт, приходите позже\", рядом работает команда догисов-инженеров {DOGGIE}");
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
