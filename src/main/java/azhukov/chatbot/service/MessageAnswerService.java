package azhukov.chatbot.service;

import azhukov.chatbot.constants.MessageType;
import azhukov.chatbot.dto.*;
import azhukov.chatbot.service.auth.AuthService;
import lombok.RequiredArgsConstructor;
import org.eclipse.jetty.util.StringUtil;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class MessageAnswerService {

    private static final String GREETING = "привет";
    private static final String DOGGIE_SMILE = ":doggie:";
    private static final String YOU_DOGGIE = "ты догги";
    private static final String[] BAN_TEXT_PARTS = {"bit.ly/"};
    private static final long TEN_YEARS_IN_SECONDS = TimeUnit.DAYS.toSeconds(365 * 10);

    private final AuthService authService;

    public List<ReqGg> answer(RespGgMessage message) {
        String text = message.getText();
        if (StringUtil.isBlank(text)) {
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

//    public ReqGgMessage answerBanResult(RespUserBan message) {
//        if (message.getModerName().equals(authService.getLogin()) && message.getReason() != null && message.getReason().contains("Автоматический бан")) {
//            return new ReqGgMessage(message.getChannelId(), "Боты банят ботов :doggie:", false, false);
//        }
//        return null;
//    }

    private ReqGgMessage answerMessage(RespGgMessage message, String text, String lowerCase) {
        if (lowerCase.contains(YOU_DOGGIE)) {
            return createSimpleReqGgMessage(message, ThreadLocalRandom.current().nextInt(99) > 20 ? ":doggie:" : "сам догги)");
        }

        ReqGgMessage withoutCurrentUser = withoutCurrentUser(message, text, lowerCase);
        if (withoutCurrentUser != null) {
            return withoutCurrentUser;
        }

        return null;
    }

    private List<ReqGg> answerBan(RespGgMessage message, String text, String lowerCase) {
        if (isCurrentUser(message)) {
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


    private ReqGgMessage withoutCurrentUser(RespGgMessage message, String text, String lowerCase) {
        if (isCurrentUser(message)) {
            return null;
        }
        if (lowerCase.contains(GREETING)) {
            return createSimpleReqGgMessage(message, "приветики :doggie:");
        }
        if (lowerCase.contains(DOGGIE_SMILE)) {
            return createSimpleReqGgMessage(message, ":doggie:");
        }

        return null;
    }

    private boolean isCurrentUser(RespGgMessage message) {
        return message.getUserName().equals(authService.getLogin());
    }

    private ReqGgMessage createSimpleReqGgMessage(RespGgMessage message, String text) {
        return new ReqGgMessage(message.getChannelId(), message.getUserName() + ", " + text, false, false);
    }


}
