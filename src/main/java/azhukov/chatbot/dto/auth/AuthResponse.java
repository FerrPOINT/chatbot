package azhukov.chatbot.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
public class AuthResponse {

    @JsonProperty("code")
    private Integer code;
    @JsonProperty("user_id")
    private String userId;
    @JsonProperty("login_page")
    private String loginPage;
    @JsonProperty("settings")
    private String settings;
    @JsonProperty("token")
    private String token;
    @JsonProperty("result")
    private boolean result;
    @JsonProperty("return")
    private boolean aReturn;
    @JsonProperty("response")
    private String response;

}
