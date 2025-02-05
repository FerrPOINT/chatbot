package azhukov.chatbot.service.webclient;

import azhukov.chatbot.dto.ChatRequest;
import azhukov.chatbot.dto.ChatResponse;
import azhukov.chatbot.service.CommonChatService;
import azhukov.chatbot.service.MappingService;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.Compression;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import okhttp3.OkHttpClient;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;

@RequiredArgsConstructor
@Slf4j
@Component
public class DiscordWebClient {

    private final MappingService mappingService;
    private final CommonChatService commonChatService;
    private final DiscordProperties properties;

    private JDA jda;

    @PostConstruct
    void init() {
        if ("disabled".equals(properties.getToken())) {
            return;
        }

        // Настройка прокси (замените адрес и порт на свои)
        Proxy proxy = properties.getProxyHost() == null ? Proxy.NO_PROXY : new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(properties.getProxyHost(), properties.getProxyPort()));

        // Создаём OkHttpClient с настройками прокси
        OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
        httpClientBuilder.proxy(proxy);

        // Инициализация JDA с использованием прокси
        jda = JDABuilder.createDefault(properties.getToken())
                .disableCache(CacheFlag.MEMBER_OVERRIDES, CacheFlag.VOICE_STATE, CacheFlag.ACTIVITY, CacheFlag.STICKER, CacheFlag.ROLE_TAGS, CacheFlag.FORUM_TAGS, CacheFlag.ONLINE_STATUS, CacheFlag.CLIENT_STATUS)
                .setBulkDeleteSplittingEnabled(false)
                .setCompression(Compression.NONE)
                .setActivity(Activity.playing("собачьи дела"))
                .setStatus(OnlineStatus.ONLINE)
                .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                .addEventListeners(new MessageListener())
                .setEnableShutdownHook(true)
                .setAutoReconnect(true)
                .setHttpClient(httpClientBuilder.build()) // Передаём кастомный HTTP клиент
                .setWebsocketFactory(new WebSocketFactory() {
                    @Override
                    public WebSocket createSocket(String url) throws IOException {
                        WebSocketFactory factory = new WebSocketFactory();
                        factory.setVerifyHostname(false);  // ОТКЛЮЧАЕТ ПРОВЕРКУ HOSTNAME
                        return factory.createSocket(url);
                    }
                })
                .build();
    }

    @PreDestroy
    void shutdown() {
        if (jda != null) {
            jda.shutdown();
        }
    }

    private class MessageListener extends ListenerAdapter {

        @Override
        public void onMessageReceived(MessageReceivedEvent event) {
            if (!event.isFromType(ChannelType.TEXT)) {
                return;
            }

            MessageChannelUnion channel = event.getChannel();

            log.info("[{}][{}] {}: {}",
                    event.getGuild().getName(),
                    channel.asTextChannel().getName(), event.getMember().getEffectiveName(),
                    event.getMessage().getContentDisplay()
            );

            ChatRequest request = mappingService.mapDis(event);

            ChatResponse chatResponse = commonChatService.answerMessage(request, request.getText(), request.getText().toLowerCase());

            if (chatResponse != null) {
                String mapped = mappingService.mapDis(chatResponse);
                if (chatResponse.getTargetUser() != null) {
                    mapped = chatResponse.getTargetUser() + ", " + mapped;
                }
                channel.sendMessage(mapped)
                        .complete();
            }
        }

    }

    public void forceReconnect() {
        shutdown();
        init();
    }

}
