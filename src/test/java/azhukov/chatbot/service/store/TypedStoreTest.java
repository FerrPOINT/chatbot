package azhukov.chatbot.service.store;

import org.junit.jupiter.api.Test;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import static org.junit.jupiter.api.Assertions.*;

class TypedStoreTest {

    @Test
    void test() {
        DB db = DBMaker.memoryDB().closeOnJvmShutdown().make();
        TypedStore<Data> dataTypedStore = new TypedStore<>(() -> db, "test", Data.class);
        dataTypedStore.clear();
        Data data = dataTypedStore.get("key1");
        assertNull(data);
        data = new Data();
        data.setVal1(100500);
        data.setVal2("somevalue");
        dataTypedStore.put("key1", data);
        db.commit();
        Data dataStored = dataTypedStore.get("key1");
        assertNotNull(dataStored);
        assertEquals(data.getVal1(), dataStored.getVal1());
        assertEquals(data.getVal2(), dataStored.getVal2());
    }

    @lombok.Data
    public static class Data {
        int val1;
        String val2;
    }
}