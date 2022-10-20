package azhukov.chatbot.dto.gg;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RespRemoveMessage {

    @JsonProperty("channel_id")
    private int channelId;
    @JsonProperty("message_id")
    private long messageId;
    @JsonProperty("adminName")
    private String adminName;

}
