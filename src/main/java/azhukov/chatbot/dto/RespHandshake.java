package azhukov.chatbot.dto;

import lombok.Data;

@Data
public class RespHandshake {

    private String protocolVersion;
    private String serverIdent;

}
