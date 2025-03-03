package azhukov.chatbot.service.webclient;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties("auth")
public class GgAuthProperties {

    private String login;
    private String password;

}
