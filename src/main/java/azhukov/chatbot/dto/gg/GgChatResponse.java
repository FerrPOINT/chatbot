package azhukov.chatbot.dto.gg;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GgChatResponse {

    @JsonProperty("channel_id")
    private final int channelId;
    @JsonProperty("text")
    private final String text;
    @JsonProperty("hideIcon")
    private final boolean hideIcon;
    @JsonProperty("mobile")
    private final boolean mobile;
}
