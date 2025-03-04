package azhukov.chatbot.service.webclient;

import azhukov.chatbot.service.gg.GgMessagesHandlerService;
import azhukov.chatbot.service.messages.RequestContext;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Финальный пример, без Mono.
 * Начиная со Spring 6 (Boot 3) AbstractWebSocketClient/StandardWebSocketClient
 * сами возвращают CompletableFuture<WebSocketSession>.
 * Т.е. метод execute(...) уже отдаёт CompletableFuture, без реактивного Mono.
 * Логика reconnect, задержка в сообщениях — остаются.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GgWebClient {

    private static final int MILLIS_BETWEEN_RESPONSE = 2000;

    private final GgMessagesHandlerService ggMessagesHandler;
    private final GgAuthProperties ggAuthProperties;

    /**
     * StandardWebSocketClient без Jetty.
     */
    private StandardWebSocketClient client;

    /**
     * Храним future, чтобы при необходимости проверять состояние или реагировать на ошибки.
     */
    private CompletableFuture<WebSocketSession> handshakeFuture;

    private volatile boolean transportError;
    private volatile boolean connectionClosed;

    private long lastMessageTime;
    private volatile long lastPingTime;

    /**
     * Инициализация после полной готовности контекста.
     */
    @EventListener(ContextRefreshedEvent.class)
    public void initAsync() {
        if (ggAuthProperties.getLogin() == null || ggAuthProperties.getPassword() == null) {
            log.warn("Не заполнены ggAuthProperties. Инициализация пропущена.");
            return;
        }
        log.info("Инициализация GG WebSocket клиента (StandardWebSocketClient + CompletableFuture) без Mono...");
        this.client = createWebSocketClient();
        log.info("GG WebSocket клиент инициализирован.");
    }

    /**
     * При остановке приложения нет явного stop(),
     * но можем обнулить future и вывести в лог.
     */
    @PreDestroy
    public void shutdown() {
        log.info("Остановка GG WebSocket клиента...");
        this.handshakeFuture = null;
    }

    /**
     * Периодический опрос каждые 10 минут.
     */
    @Scheduled(cron = "0 */10 * ? * *")
    public synchronized void reconnect() {
        if (ggAuthProperties.getLogin() == null || ggAuthProperties.getPassword() == null) {
            return;
        }
        if (client == null) {
            log.info("RECREATE START - нет клиента");
            this.client = createWebSocketClient();
            log.info("RECREATE COMPLETE");
            return;
        }

        // Проверяем последний ping
        if (lastPingTime != 0) {
            final long lastPingHours = TimeUnit.MILLISECONDS.toHours(lastPingTime);
            final long currentHours = TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis());
            if (currentHours - lastPingHours > 1) {
                log.info("RECONNECT START (last ping > 1 hour)");
                forceReconnect();
                log.info("RECONNECT COMPLETE");
                return;
            }
        }
        // Если были ошибки
        if (transportError || connectionClosed) {
            log.info("RECONNECT AFTER ERROR START");
            forceReconnect();
            log.info("RECONNECT AFTER ERROR COMPLETE");
        }
    }

    /**
     * Принудительный reconnect.
     */
    public void forceReconnect() {
        log.info("FORCE RECONNECT");
        this.client = createWebSocketClient();
    }

    /**
     * Создаём новый клиент и вызываем execute(...), который теперь сам возвращает CompletableFuture.
     */
    private StandardWebSocketClient createWebSocketClient() {
        StandardWebSocketClient newClient = new StandardWebSocketClient();

        // Начиная со Spring 6, execute(...) возвращает CompletableFuture<WebSocketSession>
        CompletableFuture<WebSocketSession> future = newClient.execute(
                new AbstractWebSocketHandler() {

                    @Override
                    public void afterConnectionEstablished(WebSocketSession session) {
                        log.info("GG connection established, session {}", session.getUri());
                        transportError = false;
                        connectionClosed = false;
                    }

                    @Override
                    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                        if (message.getPayloadLength() < 1) {
                            log.error("Empty payload for session: {}", session.getAttributes());
                            return;
                        }
                        RequestContext requestContext = new RequestContext();
                        List<String> responses = ggMessagesHandler.handleResponse(message.getPayload(), requestContext);
                        if (responses != null) {
                            for (String resp : responses) {
                                sendMessageWithDelay(session, resp);
                            }
                        }
                        if (requestContext.getLastPingTime() != 0) {
                            lastPingTime = requestContext.getLastPingTime();
                        }
                    }

                    @Override
                    public void handleTransportError(WebSocketSession session, Throwable exception) {
                        transportError = true;
                        log.error("Transport error", exception);
                    }

                    @Override
                    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
                        connectionClosed = true;
                        log.error("Connection closed with code: {}, reason: {}",
                                status.getCode(), status.getReason());
                    }
                },
                "wss://chat-1.goodgame.ru/chat2/"
        );

        future.whenComplete((session, throwable) -> {
            if (throwable != null) {
                // Ошибка подключения
                transportError = true;
                log.error("Connection error in handshake", throwable);
            } else {
                // Успешное подключение
                log.info("WebSocket session is now open: {}, {}",
                        session.getUri(), session.getAttributes());
            }
        });

        this.handshakeFuture = future;
        return newClient;
    }

    /**
     * Отправляем сообщения с небольшими интервалами, чтобы не спамить.
     */
    private synchronized void sendMessageWithDelay(WebSocketSession session, String message) {
        try {
            long delay = Math.max(0, MILLIS_BETWEEN_RESPONSE - (System.currentTimeMillis() - lastMessageTime));
            if (delay > 0) {
                Thread.sleep(delay);
            }
            session.sendMessage(new TextMessage(message));
            lastMessageTime = System.currentTimeMillis();
        } catch (Exception e) {
            log.error("Ошибка при отправке сообщения", e);
        }
    }
}
