package azhukov.chatbot.service.prediction;

import lombok.Data;

import java.util.List;

@Data
public class Prediction {

    private String id;
    private List<String> mainMessages;
    private List<String> repeatMessages;

}
