package azhukov.chatbot.service.webclient;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties("discord")
public class DiscordProperties {

    private String token;
    private String proxyHost;
    private int proxyPort;

}
