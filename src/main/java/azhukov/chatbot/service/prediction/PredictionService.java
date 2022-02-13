package azhukov.chatbot.service.prediction;

import azhukov.chatbot.service.Randomizer;
import azhukov.chatbot.service.store.DailyStore;
import azhukov.chatbot.service.store.Store;
import azhukov.chatbot.util.IOUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PredictionService {

    private static final String PREDICTION_KEY = "PREDICTION";

    private final ObjectMapper objectMapper;
    private final DailyStore dailyStore;

    private final Map<String, Prediction> predictionsById = new HashMap<>();
    private final List<Prediction> predictions = new ArrayList<>();

    @PostConstruct
    void init() {
        try {
            IOUtils.listFilesFromResources("predictions", ".json", inputStream -> {
                try {
                    final Prediction prediction = objectMapper.readValue(inputStream, Prediction.class);
                    this.predictions.add(prediction);
                    if (predictionsById.put(prediction.getId(), prediction) != null) {
                        throw new IllegalStateException("Duplicate id: " + prediction.getId());
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            if (predictions.isEmpty()) {
                throw new IllegalStateException("predictions are empty");
            }

            Collections.shuffle(this.predictions);
        } catch (Exception e) {
            throw new IllegalStateException("While reading predictions", e);
        }
    }

    public String predict(String user) {
        final Store store = dailyStore.getStore(PREDICTION_KEY);
        final String key = store.get(user);
        if (key == null) {
            final Prediction prediction = Randomizer.getRandomItem(predictions);
            store.put(user, prediction.getId());
            return Randomizer.getRandomItem(prediction.getMainMessages());
        } else {
            return "На сегодня ваше предсказание - " + Randomizer.getRandomItem(predictionsById.get(key).getRepeatMessages());
        }
    }

}
