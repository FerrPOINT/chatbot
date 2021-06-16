package azhukov.chatbot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ReqDeleteMessage {

    @JsonProperty("channel_id")
    private final String channelId;
    @JsonProperty("message_id")
    private final long messageId;
    @JsonProperty("adminName")
    private final String adminName;

}
