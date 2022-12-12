package azhukov.chatbot.service.dunge.data;

import azhukov.chatbot.db.DbService;
import azhukov.chatbot.db.DbType;
import azhukov.chatbot.service.store.TypedStore;
import org.springframework.stereotype.Service;

@Service
public class HeroInfoStore extends TypedStore<HeroInfo> {

    public HeroInfoStore(DbService dbService) {
        super(() -> dbService.getDb(DbType.DUNGE), DbType.DUNGE.name(), HeroInfo.class);
    }

}
