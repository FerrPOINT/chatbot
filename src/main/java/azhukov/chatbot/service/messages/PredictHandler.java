package azhukov.chatbot.service.messages;

import azhukov.chatbot.dto.ChatRequest;
import azhukov.chatbot.dto.ChatResponse;
import azhukov.chatbot.service.prediction.PredictionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PredictHandler extends MessageHandler {

    private static final String[] PREDICT_MESSAGES = {"!предсказания", "!предсказание", "!predict", "!погадай", "!гадать", "!гадание", "!погадать"};

    @Autowired
    private PredictionService predictionService;

    @Override
    public ChatResponse answerMessage(ChatRequest message, String text, String lowerCase) {
        if (message.getUserName() != null) {
            for (String predictMessage : PREDICT_MESSAGES) {
                if (lowerCase.contains(predictMessage)) {
                    return createUserMessage(message, predictionService.predict(message.getUserName()));
                }
            }
        }
        return null;
    }

}
