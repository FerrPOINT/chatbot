package azhukov.chatbot.dto;

import lombok.Data;

import java.util.List;

@Data
public class RespChannelsList {
    private List<Channel> channels;
}