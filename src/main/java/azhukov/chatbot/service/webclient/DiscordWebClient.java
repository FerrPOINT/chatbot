package azhukov.chatbot.service.webclient;

import azhukov.chatbot.dto.ChatRequest;
import azhukov.chatbot.dto.ChatResponse;
import azhukov.chatbot.service.CommonChatService;
import azhukov.chatbot.service.MappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.Compression;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@RequiredArgsConstructor
@Slf4j
@Component
public class DiscordWebClient {

    private final MappingService mappingService;
    private final CommonChatService commonChatService;

    @Value("${discord.token:disabled}")
    private String token;
    private JDA jda;

    @PostConstruct
    void init() {
        if ("disabled".equals(token)) {
            return;
        }
        jda = JDABuilder.createDefault(token)
                .disableCache(CacheFlag.MEMBER_OVERRIDES, CacheFlag.VOICE_STATE, CacheFlag.ACTIVITY, CacheFlag.STICKER, CacheFlag.ROLE_TAGS, CacheFlag.FORUM_TAGS, CacheFlag.ONLINE_STATUS, CacheFlag.CLIENT_STATUS)
                .setBulkDeleteSplittingEnabled(false)
                .setCompression(Compression.NONE)
                .setActivity(Activity.playing("собачьи дела"))
                .setStatus(OnlineStatus.ONLINE)
                .enableIntents(GatewayIntent.GUILD_MESSAGES)
                .enableIntents(GatewayIntent.DIRECT_MESSAGES)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .addEventListeners(new MessageListener())
                .setEnableShutdownHook(true)
                .setAutoReconnect(true)
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
            if (event.isFromType(ChannelType.PRIVATE)) {
                log.info("[PM] {}: {}", event.getAuthor().getName(), event.getMessage().getContentDisplay());
            } else {
                log.info("[{}][{}] {}: {}",
                        event.getGuild().getName(),
                        event.getChannel().asTextChannel().getName(), event.getMember().getEffectiveName(),
                        event.getMessage().getContentDisplay()
                );

                ChatRequest request = mappingService.mapDis(event);

                ChatResponse chatResponse = commonChatService.answerMessage(request, request.getText(), request.getText().toLowerCase());

                if (chatResponse != null) {
                    String mapped = mappingService.mapDis(chatResponse);
                    if (chatResponse.getTargetUser() != null) {
                        mapped = chatResponse.getTargetUser() + ", " + mapped;
                    }
                    event.getChannel()
                            .sendMessage(mapped)
                            .complete();
                }

            }
        }
    }

}
