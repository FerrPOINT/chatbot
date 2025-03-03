package azhukov.chatbot.service.webclient;

import azhukov.chatbot.service.gg.GgMessagesHandlerService;
import azhukov.chatbot.service.messages.RequestContext;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.jetty.JettyWebSocketClient;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class GgWebClient {

    private static final int MILLIS_BETWEEN_RESPONSE = 2000;

    private final GgMessagesHandlerService ggMessagesHandler;
    private final GgAuthProperties ggAuthProperties;

    private ChatbotWebSocketClient client;
    private boolean transportError;
    private boolean connectionClosed;

    private long lastMessageTime;

    @PostConstruct
    public void initAsync() {
        CompletableFuture.runAsync(this::init);
    }

    @Async
    public void init() {
        if (ggAuthProperties.getLogin() == null || ggAuthProperties.getPassword() == null) {
            return;
        }
        client = createWebSocketClient();
    }

    @PreDestroy
    void shutdown() {
        try {
            client.stop();
        } catch (Exception ignored) {
        }
    }

    //every 10 mins
    @Scheduled(cron = "0 */10 * ? * *")
    @Async
    synchronized void reconnect() {
        if (ggAuthProperties.getLogin() == null || ggAuthProperties.getPassword() == null) {
            return;
        }
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

    public ChatbotWebSocketClient createWebSocketClient() {
        ChatbotWebSocketClient client = new ChatbotWebSocketClient();
        client.start();
        client.doHandshake(new AbstractWebSocketHandler() {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                log.info("GG connection established, session {}", session.getUri());
            }

            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                if (message.getPayloadLength() < 1) {
                    log.error("Empty payload for session: {}", session.getAttributes());
                }
                final RequestContext requestContext = new RequestContext();
                List<String> response = ggMessagesHandler.handleResponse(message.getPayload(), requestContext);
                if (response != null) {
                    for (String resp : response) {
                        sendMessageWithDelay(session, resp);
                    }
                }

                if (requestContext.getLastPingTime() != 0) {
                    client.setLastPingTime(requestContext.getLastPingTime());
                }
            }

            @Override
            public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
                transportError = true;
                log.error("Transport error", exception);
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
                connectionClosed = true;
                log.error("Connection closed with code: {}, reason: {}", status.getCode(), status.getReason());
            }

        }, "wss://chat-1.goodgame.ru/chat2/");

        this.transportError = false;
        this.connectionClosed = false;
        this.lastMessageTime = System.currentTimeMillis();
        return client;
    }

    @Async
    public void forceReconnect() {
        if (client.isRunning()) {
            client.stop();
        }
        log.info("FORCE RECONNECT");
        client = createWebSocketClient();
    }

    @SneakyThrows
    private synchronized void sendMessageWithDelay(WebSocketSession session, String message) {
        long delay = Math.max(0, MILLIS_BETWEEN_RESPONSE - (System.currentTimeMillis() - lastMessageTime));
        if (delay > 0) {
            Thread.sleep(delay);
        }
        session.sendMessage(new TextMessage(message));
        lastMessageTime = System.currentTimeMillis();
    }

    private static class ChatbotWebSocketClient extends JettyWebSocketClient {
        @Getter
        @Setter
        long lastPingTime;
    }
}