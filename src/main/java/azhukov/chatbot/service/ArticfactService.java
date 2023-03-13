package azhukov.chatbot.service;

import azhukov.chatbot.service.dunge.data.Artifact;
import azhukov.chatbot.util.IOUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticfactService {

    private final ObjectMapper objectMapper;

    private final Map<String, Artifact> idToItem = new HashMap<>();
    @Getter
    private final List<Artifact> items = new ArrayList<>();

    @PostConstruct
    void init() {
        try {
            IOUtils.listFilesFromResources("artifacts", ".json", inputStream -> {
                try {
                    final Artifact data = objectMapper.readValue(inputStream, Artifact.class);
                    items.add(data);
                    if (idToItem.put(data.getId(), data) != null) {
                        throw new IllegalStateException("Duplicate id: " + data.getId());
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            if (items.isEmpty()) {
                throw new IllegalStateException("Items are empty");
            }
            Collections.shuffle(items);
        } catch (Exception e) {
            throw new IllegalStateException("While reading items", e);
        }
    }

    public Artifact getById(String id) {
        return idToItem.get(id);
    }


}
