package azhukov.chatbot.service;

import azhukov.chatbot.service.dictionary.Dictionary;
import azhukov.chatbot.service.dictionary.DictionaryService;
import azhukov.chatbot.service.users.UserCollectionStore;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Set;

@RequiredArgsConstructor
@Service
public class CommandsPermissionsService {

    private final UserCollectionStore userCollectionStore;
    @Lazy
    @Autowired
    private DictionaryService dictionaryService;

    public boolean isPermitted(String command, String user) {
        if ("!луна".equals(command)) {
            Dictionary sailor = dictionaryService.getById("sailor");
            Set<String> currentSet = userCollectionStore.getCurrentSet(user, sailor.getId());
            return currentSet != null && currentSet.size() == sailor.getData().size();
        }
        return false;
    }

}
