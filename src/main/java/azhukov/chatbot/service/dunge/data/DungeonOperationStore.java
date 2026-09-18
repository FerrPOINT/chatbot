package azhukov.chatbot.service.dunge.data;

import azhukov.chatbot.db.DbService;
import azhukov.chatbot.db.DbType;
import azhukov.chatbot.service.store.TypedStore;
import org.springframework.stereotype.Repository;

@Repository
public class DungeonOperationStore extends TypedStore<DungeonOperation> {
    public DungeonOperationStore(DbService dbService) {
        super(() -> dbService.getDb(DbType.DUNGE_ECONOMY), "DUNGE_OPERATIONS", DungeonOperation.class);
    }
}
