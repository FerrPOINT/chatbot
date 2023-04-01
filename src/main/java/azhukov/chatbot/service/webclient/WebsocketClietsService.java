package azhukov.chatbot.service.webclient;

import azhukov.chatbot.service.gg.GgMessagesHandlerService;
import azhukov.chatbot.service.messages.RequestContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class WebsocketClietsService {

    private final GgMessagesHandlerService ggMessagesHandler;

    private ChatbotWebSocketClient client;
    private DiscordWebClient discordClient;
    private boolean transportError;
    private boolean connectionClosed;

    @PostConstruct
    void init() {
        client = createWebSocketClient();
    }

    @PreDestroy
    void shutdown() {
        client.stop();
    }

    //every 10 mins
    @Scheduled(cron = "0 */10 * ? * *")
    synchronized void reconnect() {
        if (!client.isRunning()) {
            log.info("RECREATE START");
            client = createWebSocketClient();
            log.info("RECREATE COMPLETE");
        } else {
            if (client.getLastPingTime() != 0) {
                final long last = TimeUnit.MILLISECONDS.toHours(client.getLastPingTime());
                final long current = TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis());
                if (current - last > 1) {
                    log.info("RECONNECT START");
                    client.stop();
                    client = createWebSocketClient();
                    log.info("RECONNECT COMPLETE");
                }
            }
            if (transportError || connectionClosed) {
                log.info("RECONNECT AFTER ERROR START");
                client.stop();
                client = createWebSocketClient();
                log.info("RECONNECT AFTER ERROR COMPLETE");
            }
        }
    }

    //every day 6:00
    @Scheduled(cron = "0 0 6 * * ?")
    synchronized void forceReconnect() {
        if (client.isRunning()) {
            client.stop();
        }
        log.info("FORCE RECONNECT");
        client = createWebSocketClient();
        discordClient.shutdown();
        discordClient.init();
    }

    public ChatbotWebSocketClient createWebSocketClient() {
        ChatbotWebSocketClient client = new ChatbotWebSocketClient();
        client.start();
        client.doHandshake(new AbstractWebSocketHandler() {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                log.info("GG connection established, session {}", session.getUri());
            }

            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
                if (message.getPayloadLength() < 1) {
                    log.error("Empty payload for session: {}", session.getAttributes());
                }
                final RequestContext requestContext = new RequestContext();
                List<String> response = ggMessagesHandler.handleResponse(message.getPayload(), requestContext);
                if (response != null) {
                    for (String resp : response) {
                        session.sendMessage(new TextMessage(resp));
                    }
                }

                if (requestContext.getLastPingTime() != 0) {
                    client.setLastPingTime(requestContext.getLastPingTime());
                }

            }

            public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
                transportError = true;
                log.error("Transport error", exception);
            }

            public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
                connectionClosed = true;
                log.error("Connection closed with code: {}, reason: {}", status.getCode(), status.getCode());
            }

        }, "wss://chat-1.goodgame.ru/chat2/");

        this.transportError = false;
        this.connectionClosed = false;
        return client;
    }

}
