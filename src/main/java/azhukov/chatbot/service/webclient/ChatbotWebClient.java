package azhukov.chatbot.service.webclient;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.socket.client.jetty.JettyWebSocketClient;


public class ChatbotWebClient extends JettyWebSocketClient {

    @Getter
    @Setter
    long lastPingTime;

}
