package azhukov.chatbot.dto.gg;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RespUserBan {

    @JsonProperty("channel_id")
    private int channelId;
    @JsonProperty("user_id")
    private long userId;
    @JsonProperty("user_name")
    private String userName;
    @JsonProperty("moder_id")
    private long moderId;
    @JsonProperty("moder_name")
    private String moderName;
    @JsonProperty("moder_rights")
    private int moderRights;
    @JsonProperty("moder_premium")
    private boolean moderPremium;
    @JsonProperty("duration")
    private int duration;
    @JsonProperty("type")
    private int type;
    @JsonProperty("reason")
    private String reason;
    @JsonProperty("show")
    private boolean show;

}
