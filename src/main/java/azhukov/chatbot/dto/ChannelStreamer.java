package azhukov.chatbot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

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
