package azhukov.chatbot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ReqGgMessage {

    @JsonProperty("channel_id")
    private final int channelId;
    @JsonProperty("text")
    private final String text;
    @JsonProperty("hideIcon")
    private final boolean hideIcon;
    @JsonProperty("mobile")
    private final boolean mobile;
}
