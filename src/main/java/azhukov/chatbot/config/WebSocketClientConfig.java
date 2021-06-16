package azhukov.chatbot.config;

import azhukov.chatbot.service.GgMessagesHandlerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.jetty.JettyWebSocketClient;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.util.List;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class WebSocketClientConfig {

    private final ObjectMapper objectMapper;
    private final GgMessagesHandlerService ggMessagesHandler;

    @Bean(destroyMethod = "stop")
    public JettyWebSocketClient jettyWebSocketClient() {
        JettyWebSocketClient client = new JettyWebSocketClient();
        client.start();
        client.doHandshake(new AbstractWebSocketHandler() {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                log.info("GG connection Established, session {}", session.getAttributes());
            }

            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
                if (message.getPayloadLength() < 1) {
                    log.error("Empty payload for session: {}", session.getAttributes());
                }
                List<String> response = ggMessagesHandler.handleResponse(message.getPayload());
                if (response != null) {
                    for (String resp : response) {
                        session.sendMessage(new TextMessage(resp));
                    }
                }
            }
        }, "wss://chat-1.goodgame.ru/chat2/");
        return client;
    }

}
