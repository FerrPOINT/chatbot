package azhukov.chatbot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class RespChatJoin {

    @JsonProperty("channel_id")
    private int channelId;
    @JsonProperty("channel_name")
    private String channelName;
    @JsonProperty("channel_streamer")
    private ChannelStreamer channelStreamer;
    @JsonProperty("motd")
    private String motd;
    @JsonProperty("clients_in_channel")
    private int clientsInChannel;
    @JsonProperty("users_in_channel")
    private int usersInChannel;
    @JsonProperty("user_id")
    private int userId;
    @JsonProperty("name")
    private String name;
    @JsonProperty("access_rights")
    private int accessRights;
    @JsonProperty("premium_only")
    private int premiumOnly;
    @JsonProperty("premium")
    private boolean premium;
    //  private List<Integer> premiums; "premiums": [],
    //  private Object notifies;  "notifies": {},
    //  private Object resubs;  "resubs": {},
    @JsonProperty("staff")
    private int staff;
    @JsonProperty("is_banned")
    private boolean isBanned;
    @JsonProperty("banned_time")
    private int bannedTime;
    @JsonProperty("reason")
    private String reason;
    @JsonProperty("permanent")
    private boolean permanent;
    @JsonProperty("payments")
    private int payments;
    //  private List<Object> paidsmiles;    "paidsmiles": []

}
