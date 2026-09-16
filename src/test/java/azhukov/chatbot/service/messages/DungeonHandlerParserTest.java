package azhukov.chatbot.service.messages;

import azhukov.chatbot.dto.ChatRequest;
import azhukov.chatbot.dto.ChatResponse;
import azhukov.chatbot.service.dunge.service.BossService;
import azhukov.chatbot.service.dunge.service.DungeonService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DungeonHandlerParserTest {
    @Test
    void parsesOneExactCommandAndArguments() {
        DungeonHandler.ParsedCommand command = DungeonHandler.ParsedCommand.parse("  !ОСАДА 25  ");
        assertEquals("!осада", command.command());
        assertArrayEquals(new String[]{"25"}, command.args());
    }

    @Test
    void doesNotTreatChatSubstringAsCommand() {
        assertNull(DungeonHandler.ParsedCommand.parse("пожалуйста !данж"));
        assertNull(DungeonHandler.ParsedCommand.parse("без команды"));
    }

    @Test
    void rejectsOverflowExtraArgumentsAndMultipleCommandsWithoutExecuting() {
        DungeonService dungeon = mock(DungeonService.class);
        DungeonHandler handler = new DungeonHandler(dungeon, mock(BossService.class));
        ChatRequest request = new ChatRequest("hero", "", false, false);

        ChatResponse overflow = handler.answerMessage(request, "!осада 999999999999999999999", "!осада 999999999999999999999");
        ChatResponse extra = handler.answerMessage(request, "!рывок now", "!рывок now");
        ChatResponse multiple = handler.answerMessage(request, "!данж !босс", "!данж !босс");

        assertTrue(overflow.getText().contains("слишком большая"));
        assertTrue(extra.getText().contains("нет аргументов"));
        assertTrue(multiple.getText().contains("нет аргументов"));
        verifyNoInteractions(dungeon);
    }
}
