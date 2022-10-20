package azhukov.chatbot.dto.gg;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ReqBan {

    @JsonProperty("userId")
    private final int userId;
    @JsonProperty("roomId")
    private final String roomId;
    @JsonProperty("reason")
    private final String reason;
    @JsonProperty("comment")
    private final String comment;
    @JsonProperty("type")
    private final Integer type;
    @JsonProperty("duration")
    private final Integer duration;
    @JsonProperty("deleteMessage")
    private final Boolean deleteMessage;

}
