package azhukov.chatbot.service.messages;

import azhukov.chatbot.dto.ChatRequest;
import azhukov.chatbot.dto.ChatResponse;
import azhukov.chatbot.service.dunge.service.BossService;
import azhukov.chatbot.service.dunge.service.DungeonService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DungeonHandler extends MessageHandler {

    private final DungeonService dungeonService;
    private final BossService bossService;

    private static final List<String> COMMANDS = List.of("!данж", "!dungeon", "!данжн");
    private static final List<String> BOSS_COMMANDS = List.of("!босс", "!boss");

    @Override
    public ChatResponse answerMessage(ChatRequest message, String text, String lowerCase) {
        if (lowerCase != null) {
            if (lowerCase.contains("!данж1")) {
                return createMessage(message, dungeonService.getDungeonResponse1(message));
            }
            if (lowerCase.contains("!резет1")) {
                dungeonService.reset();
                return createMessage(message, "резет {DOGGIE}");
            }
            for (String command : COMMANDS) {
                if (lowerCase.contains(command)) {
                    return createMessage(message, dungeonService.getDungeonResponse(message));
                }
            }
            for (String command : BOSS_COMMANDS) {
                if (lowerCase.contains(command)) {
                    return createMessage(message, bossService.getCurrentBossData());
                }
            }
        }
        return null;
    }
}
