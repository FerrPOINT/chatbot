package azhukov.chatbot.dto.gg;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ChannelStreamer {
    @JsonProperty("id")
    private long id;
    @JsonProperty("name")
    private String name;
    @JsonProperty("avatar")
    private String avatar;
    @JsonProperty("rights")
    private int rights;
    @JsonProperty("payments")
    private String payments;
    @JsonProperty("premium")
    private boolean premium;
}
