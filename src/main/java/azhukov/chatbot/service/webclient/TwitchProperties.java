package azhukov.chatbot.service.webclient;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties("twitch")
public class TwitchProperties {

    private String channel;
    private String userName;
    private String oauthToken;
    private String clientId;
    private String clientSecret;
    private String botOwnerId;

}