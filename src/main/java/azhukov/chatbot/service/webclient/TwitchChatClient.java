package azhukov.chatbot.service.webclient;

import azhukov.chatbot.dto.ChatRequest;
import azhukov.chatbot.dto.ChatResponse;
import azhukov.chatbot.service.CommonChatService;
import azhukov.chatbot.service.MappingService;
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RequiredArgsConstructor
@Component
public class TwitchChatClient {

    private final MappingService mappingService;
    private final CommonChatService commonChatService;
    private final TwitchProperties properties;
    private TwitchClient twitchClient;

    @PostConstruct
    public void initAsync() {
        CompletableFuture.runAsync(this::init);
    }

    public void init() {
        try {
            OAuth2Credential credential = new OAuth2Credential("twitch", properties.getOauthToken());

            TwitchClient prev = twitchClient;
            twitchClient = TwitchClientBuilder.builder()
                    .withEnableChat(true)
                    .withChatAccount(credential)
                    .build();

            if (prev != null) {
                try {
                    prev.close();
                } catch (Exception e) {
                    log.error("UNEXPECTED EXCEPTION", e);
                }
            }

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
        ChatRequest request = mappingService.mapTwitch(event);
        ChatResponse response = commonChatService.answerMessage(request, request.getText(), request.getText().toLowerCase());
        if (response != null) {
            twitchClient.getChat().sendMessage(event.getChannel().getName(), mappingService.mapTwitch(response));
        }
    }

}
