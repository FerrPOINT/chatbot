package azhukov.chatbot.dto.gg;

import azhukov.chatbot.constants.MessageType;
import lombok.Data;

@Data
public class ReqGg {

    private final MessageType type;

    private final Object data;

}
