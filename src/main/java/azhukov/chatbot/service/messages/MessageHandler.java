package azhukov.chatbot.service.messages;

import azhukov.chatbot.dto.ChatRequest;
import azhukov.chatbot.dto.ChatResponse;
import org.springframework.stereotype.Component;

@Component
public abstract class MessageHandler {

    public abstract ChatResponse answerMessage(ChatRequest message, String text, String lowerCase);

    protected ChatResponse createUserMessage(ChatRequest message, String text) {
        return createUserMessage(message, text, message.getUserName());
    }

    protected ChatResponse createUserMessage(ChatRequest message, String text, String user) {
        return new ChatResponse(user, text);
    }

    protected ChatResponse createMessage(ChatRequest message, String text) {
        return new ChatResponse(null, text);
    }

}
