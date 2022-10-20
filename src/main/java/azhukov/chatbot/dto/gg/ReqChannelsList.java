package azhukov.chatbot.dto.gg;

import lombok.Data;

@Data
public class ReqChannelsList {
    private final int start;
    private final int count;
}