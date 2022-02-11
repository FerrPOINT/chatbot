package azhukov.chatbot.service;

import azhukov.chatbot.constants.MessageType;
import azhukov.chatbot.dto.*;
import azhukov.chatbot.service.auth.AuthService;
import azhukov.chatbot.service.messages.MessageHandler;
import azhukov.chatbot.service.store.DailyStore;
import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class MessageAnswerService {

    private static final String[] BAN_TEXT_PARTS = {"bit.ly/"};

    private final AuthService authService;
    private final List<MessageHandler> messageHandlers;

    public List<ReqGg> answer(RespGgMessage message) {
        String text = message.getText();
        if (StringUtils.isBlank(text)) {
            return null;
        }

        text = text.trim();
        String lowerCase = text.toLowerCase();

        List<ReqGg> reqBan = answerBan(message, text, lowerCase);
        if (reqBan != null) {
            return reqBan;
        }

        ReqGgMessage reqGgMessage = answerMessage(message, text, lowerCase);
        if (reqGgMessage != null) {
            return Collections.singletonList(new ReqGg(MessageType.send_message, reqGgMessage));
        }

        return null;
    }

    private ReqGgMessage answerMessage(RespGgMessage message, String text, String lowerCase) {
        for (MessageHandler messageHandler : messageHandlers) {
            final ReqGgMessage resp = messageHandler.answerMessage(message, text, lowerCase);
            if (resp != null) {
                return resp;
            }
        }
        return null;
    }

    private List<ReqGg> answerBan(RespGgMessage message, String text, String lowerCase) {
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
                                authService.getLogin()
                        )),
                        new ReqGg(MessageType.send_message, new ReqGgMessage(message.getChannelId(), comment, false, false))
                );
            }
        }

        return null;
    }

}
