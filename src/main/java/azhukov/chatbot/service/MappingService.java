package azhukov.chatbot.service;

import azhukov.chatbot.dto.ChatRequest;
import azhukov.chatbot.dto.ChatResponse;
import azhukov.chatbot.dto.gg.GgChatRequest;
import azhukov.chatbot.dto.gg.GgChatResponse;
import azhukov.chatbot.service.auth.GgAuthService;
import azhukov.chatbot.service.macro.MacrosDictionary;
import azhukov.chatbot.service.macro.MacrosTemplate;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.springframework.stereotype.Service;

import java.util.Set;

@RequiredArgsConstructor
@Service
public class MappingService {

    private static final Set<String> GG_KEYS = MacrosDictionary.GG_DICTIONARY.keySet();
    private static final Set<String> DISCORD_KEYS = MacrosDictionary.DISCORD_DICTIONARY.keySet();

    private final GgAuthService authService;

    public GgChatResponse mapDis(GgChatRequest ggRequest, ChatResponse response) {
        String text = new MacrosTemplate(response.getText(), GG_KEYS).compileString(MacrosDictionary.GG_DICTIONARY);
        return new GgChatResponse(
                ggRequest.getChannelId(),
                response.getTargetUser() == null ? text : (response.getTargetUser() + ", " + text),
                false,
                false);
    }

    public String mapDis(ChatResponse response) {
        return new MacrosTemplate(response.getText(), DISCORD_KEYS).compileString(MacrosDictionary.DISCORD_DICTIONARY);
    }

    public ChatRequest mapDis(GgChatRequest ggRequest) {
        return new ChatRequest(
                ggRequest.getUserName(),
                ggRequest.getText(),
                authService.isCurrentUser(ggRequest),
                ggRequest.getText() != null && ggRequest.getText().contains(authService.getLogin() + ", ")
        );
    }

    public ChatRequest mapDis(MessageReceivedEvent discordRequest) {
        String userName = discordRequest.getAuthor().getName();
        String selfUser = discordRequest.getJDA().getSelfUser().getName();
        return new ChatRequest(
                userName,
                discordRequest.getMessage().getContentDisplay(),
                selfUser.equals(userName),
                discordRequest.getMessage().getMentions().getMembers().stream().anyMatch(selfUser::equals)
        );
    }

}
