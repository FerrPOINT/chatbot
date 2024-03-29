package azhukov.chatbot.dto.gg;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Setter;

@Data
public class GgChatRequest {

    @JsonProperty("channel_id")
    private int channelId;
    @JsonProperty("user_id")
    private int userId;
    @JsonProperty("user_name")
    private String userName;
    @JsonProperty("user_rights")
    private int userRights;
    @JsonProperty("premium")
    private boolean premium;
    @JsonProperty("hideIcon")
    private boolean hideIcon;
    @JsonProperty("mobile")
    private boolean mobile;
    //    private Double payments;
    //    private Object paidsmiles;
    @JsonProperty("message_id")
    private long messageId;
    @JsonProperty("timestamp")
    private long timestamp;
    //    private String color;
    @JsonProperty("text")
    private String text;

    @Setter
    private boolean currentUser;
    @Setter
    private boolean forCurrentUser;

}
