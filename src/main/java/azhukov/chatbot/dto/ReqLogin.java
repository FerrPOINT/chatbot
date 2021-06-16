package azhukov.chatbot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ReqLogin {
    // идентификатор пользователя на сайте, либо 0 для гостей
    @JsonProperty("user_id")
    private final String userId;
    // ключ авторизации. Если не указан, то будет запрошен гостевой доступ.
    @JsonProperty("token")
    private final String token;
}
