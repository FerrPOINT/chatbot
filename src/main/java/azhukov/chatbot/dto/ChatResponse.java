package azhukov.chatbot.dto;

import lombok.Data;

@Data
public class ChatResponse {

    private final String targetUser;
    private final String text;

}
