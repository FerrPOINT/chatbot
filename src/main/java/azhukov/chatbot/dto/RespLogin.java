package azhukov.chatbot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RespLogin {
    // id-пользователя на сайте, для гостей 0
    @JsonProperty("user_id")
    private String userId;
    // nick на сайте, для гостей ""
    @JsonProperty("user_name")
    private String userName;
}
