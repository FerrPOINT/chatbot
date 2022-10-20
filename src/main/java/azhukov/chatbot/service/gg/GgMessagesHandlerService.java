package azhukov.chatbot.service.gg;

import azhukov.chatbot.constants.MessageType;
import azhukov.chatbot.dto.auth.AuthResponse;
import azhukov.chatbot.dto.gg.*;
import azhukov.chatbot.service.auth.GgAuthService;
import azhukov.chatbot.service.messages.RequestContext;
import azhukov.chatbot.service.util.Randomizer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class GgMessagesHandlerService {

    private static final String LAST_KNOWN_VERSION = "2";

    private final GgAuthService authService;
    private final GgMessageAnswerService messageAnswerService;
    private final ObjectMapper objectMapper;

    @Value("${checked-channels}")
    private int channelId;

    private int messagesCounter;
    private int dogenAnswerCounter;

    public List<String> handleResponse(String message, RequestContext requestContext) {
        try {

            JsonNode jsonNode = objectMapper.readTree(message);
            JsonNode type = jsonNode.get("type");
            if (type == null) {
                log.error("No type for message: {}", message);
                return null;
            }
            MessageType messageType = MessageType.byName(type.textValue());
            if (messageType == null) {
                log.error("Unknown message type: {}", message);
                return null;
            }

            JsonNode data = jsonNode.get("data");
            if (data == null) {
                log.error("No data for message: {}", message);
                return null;
            }

            Object result = switch (messageType) {
                case welcome -> handleHandshakeData(getData(data, RespHandshake.class));
                case success_auth -> handleSuccessAuth(getData(data, RespLogin.class));
                case channels_list -> handleChannelsList(getData(data, RespChannelsList.class));
                case success_join -> handleSuccessJoin(getData(data, RespChatJoin.class));
                case channel_counters -> handleChannelCounters(data, requestContext);
                case remove_message -> handleRemoveMessage(getData(data, RespRemoveMessage.class));
                case message -> null;
                default -> {
                    log.error("Unhandled message type: {}", messageType);
                    yield  null;
                }
            };

            if (result != null) {
                String jsonResp = objectMapper.writeValueAsString(result);
                log.info("Our resp: {}", jsonResp);
                return Collections.singletonList(jsonResp);
            }

            final GgChatRequest request = getData(data, GgChatRequest.class);

            List<ReqGg> listResult = switch (messageType) {
                case message -> {
                    messagesCounter++;
                    yield handleChatMessage(request);
                }
                default -> null;
            };

            if (listResult == null && messagesCounter % 100 == 0) {
                listResult = Collections.singletonList(new ReqGg(MessageType.send_message, new GgChatResponse(request.getChannelId(), Randomizer.tossCoin() ? "Вуфь :doggie:" : "Аффь :doggie:", false, false)));
            }

            if (listResult != null) {
                ArrayList<String> jsonsList = new ArrayList<>(listResult.size());
                if (listResult.size() == 1) {
                    dogenAnswerCounter++;
                }
                for (ReqGg reqGg : listResult) {
                    String json = objectMapper.writeValueAsString(reqGg);
                    jsonsList.add(json);
                    log.info("Our resp: {}", json);
                }
                return jsonsList;
            }

            return null;
        } catch (Exception e) {
            log.error("While handling message: {}", message, e);
        }
        return null;
    }

    private <T> T getData(JsonNode message, Class<T> clazz) throws JsonProcessingException {
        return objectMapper.treeToValue(message, clazz);
    }

    public ReqGg handleHandshakeData(RespHandshake message) throws Exception {
        log.info("Handshake successful: {}", message);
        if (!LAST_KNOWN_VERSION.equals(message.getProtocolVersion())) {
            log.warn("New version available: {}", message.getProtocolVersion());
        }
        AuthResponse authData = authService.getAuthData();
        return new ReqGg(MessageType.auth, new ReqLogin(authData.getUserId(), authData.getToken()));
    }

    public ReqGg handleSuccessAuth(RespLogin message) {
        log.info("Auth successful: {}", message);
        int start = 0;
        int count = 50;
        log.info("Getting channels list: start={}, count={}", start, count);
        return new ReqGg(MessageType.get_channels_list, new ReqChannelsList(start, count));
    }

    public ReqGg handleChannelsList(RespChannelsList message) {
        log.info("Channels list: {}", message);
        return handleChatConnect();
    }

    public ReqGg handleChatConnect() {
        return new ReqGg(MessageType.join, new ReqChatJoin(channelId, false));
    }

    public List<ReqGg> handleChatMessage(GgChatRequest message) {
        log.info("{} says: {}", message.getUserName(), message.getText());
        return messageAnswerService.answer(message);
    }

    public ReqGg handleSuccessJoin(RespChatJoin message) {
        log.info("Join Success to: {}, id: {}", message.getChannelStreamer().getName(), message.getChannelStreamer().getId());
        return null;
    }

    public ReqGg handleChannelCounters(JsonNode message, RequestContext requestContext) {
        requestContext.setLastPingTime(System.currentTimeMillis());
        return null;
    }

    public ReqGg handleRemoveMessage(RespRemoveMessage message) {
        log.info("{} deleted the message with id: {}", message.getAdminName(), message.getMessageId());
        return null;
    }

}
