package azhukov.chatbot.service.dictionary;

import lombok.Data;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class Dictionary {
    private String id;
    private List<String> commandsList;
    private ConcurrentHashMap<String, String> data;
    private String prefix;
    private String repeatPrefix;
    private String postfix;
    private boolean collect;
    private boolean repeatMessage;
    private String fullCollectionMessage;
    private boolean locked;
}
