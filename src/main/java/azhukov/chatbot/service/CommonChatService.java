package azhukov.chatbot.service;

import azhukov.chatbot.dto.ChatRequest;
import azhukov.chatbot.dto.ChatResponse;
import azhukov.chatbot.service.messages.MessageHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommonChatService {

    private final SweetieService sweetieService;
    private final List<MessageHandler> messageHandlers;

    public ChatResponse answerMessage(ChatRequest message, String text, String lowerCase) {
        if (message.isCurrentUser()) {
            return null;
        }
        // TODO fix sweetie for all platforms
        sweetieService.addSweetie(message.getUserName(), message);
        for (MessageHandler messageHandler : messageHandlers) {
            final ChatResponse resp = messageHandler.answerMessage(message, text, lowerCase);
            if (resp != null) {
                return resp;
            }
        }
        return null;
    }

}