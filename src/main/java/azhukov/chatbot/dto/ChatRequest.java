package azhukov.chatbot.dto;

import lombok.Value;

@Value
public class ChatRequest {

    String userName;
    String text;
    boolean currentUser;
    boolean forCurrentUser;

}
