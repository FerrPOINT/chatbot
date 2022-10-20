package azhukov.chatbot.dto.gg;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ReqChatJoin {

    @JsonProperty("channel_id")
    private final int channelId;
    @JsonProperty("hidden")
    private final boolean hidden;

}
