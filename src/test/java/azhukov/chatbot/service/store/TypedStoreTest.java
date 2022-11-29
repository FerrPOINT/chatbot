package azhukov.chatbot.service.store;

import azhukov.chatbot.db.DbService;
import org.junit.jupiter.api.Test;

import static azhukov.chatbot.db.DbType.STORE;
import static org.junit.jupiter.api.Assertions.*;

class TypedStoreTest {

    @Test
    void test() {
        DbService dbService = new DbService();
        TypedStore<Data> dataTypedStore = new TypedStore<>(() -> dbService.getDb(STORE), "test", Data.class);
        dataTypedStore.clear();
        Data data = dataTypedStore.get("key1");
        assertNull(data);
        data = new Data();
        data.setVal1(100500);
        data.setVal2("somevalue");
        dataTypedStore.put("key1", data);
        Data dataStored = dataTypedStore.get("key1");
        assertNotNull(data);
        assertEquals(data.getVal1(), dataStored.getVal1());
        assertEquals(data.getVal2(), dataStored.getVal2());
    }

    @lombok.Data
    public static class Data {
        int val1;
        String val2;
    }
}