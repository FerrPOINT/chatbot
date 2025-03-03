package azhukov.chatbot.service.gg;

import azhukov.chatbot.constants.MessageType;
import azhukov.chatbot.dto.ChatResponse;
import azhukov.chatbot.dto.gg.*;
import azhukov.chatbot.service.CommonChatService;
import azhukov.chatbot.service.MappingService;
import azhukov.chatbot.service.auth.GgAuthService;
import azhukov.chatbot.service.webclient.GgAuthProperties;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GgMessageAnswerService {

    private static final String[] BAN_TEXT_PARTS = {"bit.ly/"};

    private final GgAuthService authService;
    private final MappingService mappingService;
    private final CommonChatService chatService;
    private final GgAuthProperties ggAuthProperties;

    public List<ReqGg> answer(GgChatRequest ggRequest) {
        String text = ggRequest.getText();
        if (StringUtils.isBlank(text)) {
            return null;
        }

        text = text.trim();
        String lowerCase = text.toLowerCase();

        List<ReqGg> reqBan = answerBan(ggRequest, text, lowerCase);
        if (reqBan != null) {
            return reqBan;
        }

        ChatResponse chatResponse = chatService.answerMessage(mappingService.mapGg(ggRequest), text, lowerCase);

        if (chatResponse != null) {
            GgChatResponse ggMessage = mappingService.mapGg(ggRequest, chatResponse);
            return Collections.singletonList(new ReqGg(MessageType.send_message, ggMessage));
        }

        return null;
    }

    private List<ReqGg> answerBan(GgChatRequest message, String text, String lowerCase) {
        if (authService.isCurrentUser(message)) {
            return null;
        }

        for (String banTextPart : BAN_TEXT_PARTS) {
            if (lowerCase.contains(banTextPart)) {
                String roomId = String.valueOf(message.getChannelId());
                String comment = "Боты банят ботов :doggie:";
                return List.of(
                        new ReqGg(MessageType.ban2, new ReqBan(
                                message.getUserId(),
                                roomId,
                                "Автоматический бан - спам по контенту " + text + ", совпадение по " + banTextPart,
                                comment,
                                2,
                                0,
                                true
                        )),
                        new ReqGg(MessageType.remove_message, new ReqDeleteMessage(
                                roomId,
                                message.getMessageId(),
                                ggAuthProperties.getLogin()
                        )),
                        new ReqGg(MessageType.send_message, new GgChatResponse(message.getChannelId(), comment, false, false))
                );
            }
        }

        return null;
    }

}
