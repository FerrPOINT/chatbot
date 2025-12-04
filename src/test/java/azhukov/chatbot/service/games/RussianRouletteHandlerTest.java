package azhukov.chatbot.service.games;

import azhukov.chatbot.dto.ChatRequest;
import azhukov.chatbot.dto.ChatResponse;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RussianRouletteHandlerTest {

    private final RussianRouletteHandler handler = new RussianRouletteHandler();

    @Test
    void testHandleRussianRoulette() {
        ChatRequest request = new ChatRequest("testUser", "!рулетка", false, false);

        ChatResponse response = handler.answerMessage(request, "!рулетка", "!рулетка");
        assertNotNull(response);
        assertEquals("testUser", response.getTargetUser());
        assertTrue(response.getText().contains("выжил! :)") || response.getText().contains("погиб! :("));
    }

    @Test
    void testHandleOtherMessage() {
        ChatRequest request = new ChatRequest("testUser", "hello", false, false);

        ChatResponse response = handler.answerMessage(request, "hello", "hello");
        assertNull(response);
    }
}
