package azhukov.chatbot.dto.gg;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Channel {

    @JsonProperty("channel_id")
    private String channelId;
    @JsonProperty("channel_name")
    private String channelName;
    @JsonProperty("clients_in_channel")
    private String clientsInChannel;
    @JsonProperty("users_in_channel")
    private String usersInChannel;

}
