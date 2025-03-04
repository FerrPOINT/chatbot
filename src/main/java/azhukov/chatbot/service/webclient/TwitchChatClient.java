package azhukov.chatbot.service.webclient;

import azhukov.chatbot.dto.ChatRequest;
import azhukov.chatbot.dto.ChatResponse;
import azhukov.chatbot.service.CommonChatService;
import azhukov.chatbot.service.MappingService;
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class TwitchChatClient {

    private final MappingService mappingService;
    private final CommonChatService commonChatService;
    private final TwitchProperties properties;
    private TwitchClient twitchClient;

    @EventListener(ContextRefreshedEvent.class)
    public void init() {
        try {
            OAuth2Credential credential = new OAuth2Credential("twitch", properties.getOauthToken());

            twitchClient = TwitchClientBuilder.builder()
                    .withEnableChat(true)
                    .withChatAccount(credential)
                    .withClientId(properties.getClientId())
                    .withClientSecret(properties.getClientSecret())
                    .withBotOwnerId(properties.getBotOwnerId())
                    .build();

            twitchClient.getEventManager().onEvent(ChannelMessageEvent.class, this::onMessageReceived);

            String channelName = properties.getChannel();
            twitchClient.getChat().joinChannel(channelName);
            log.info("Бот подключился к каналу: " + channelName);
            log.info("Twitch чат клиент успешно запущен с User Access Token.");
        } catch (Exception e) {
            log.error("Ошибка при инициализации Twitch клиента", e);
        }
    }

    @PreDestroy
    public void shutdown() {
        if (twitchClient != null) {
            twitchClient.close();
        }
    }

    private void onMessageReceived(ChannelMessageEvent event) {
        log.info("{} says: {}", event.getUser().getName(), event.getMessage());
        ChatRequest request = mappingService.mapTwitch(event);
        ChatResponse response = commonChatService.answerMessage(request, request.getText(), request.getText().toLowerCase());
        if (response != null) {
            twitchClient.getChat().sendMessage(event.getChannel().getName(), mappingService.mapTwitch(response));
        }
    }

}
