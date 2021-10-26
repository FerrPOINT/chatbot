package azhukov.chatbot.service;

import azhukov.chatbot.service.store.DailyStore;
import azhukov.chatbot.service.store.Store;

import java.util.Arrays;
import java.util.stream.Collectors;

public class TalismanChooser {

    private static final String TALISMAN_KEY = "TALISMAN";

    private static final String[] TALISMANS = {
            "петух",
            "бык",
            "змея",
            "овца",
            "кролик",
            "дракон",
            "крыса",
            "лошадь",
            "собака",
            "свинья",
            "обезьяна",
            "тигр"
    };

    public static String getTalismanMessage(String user) {
        final Store<String, String> store = DailyStore.getStore(TALISMAN_KEY);
        final String key = store.get(user);
        if (key == null) {
            final String randomTalismanKey = getRandomTalismanKey();
            store.put(user, randomTalismanKey);
            final String origMessage = getOrigMessage(randomTalismanKey);
            return origMessage + " :doggie:";
        } else {
            return "На сегодня твой талисман - " + key + " :doggie:";
        }
    }

    public static String getTalismansList() {
        return Arrays.stream(TALISMANS).collect(Collectors.joining(", "));
    }

    private static String getRandomTalismanKey() {
        return TALISMANS[Randomizer.nextInt(TALISMANS.length)];
    }

    private static String getOrigMessage(String key) {
        return switch (key) {
            case "петух" -> "Ты получаешь талисман петуха, теперь вы владеете левитацией и телекинезом!";
            case "бык" -> "Ты получаешь талисман быка, а это значит - невероятная физическая сила!";
            case "змея" -> "Ты получаешь талисман змеи, у тебя новая способность - невидимость!";
            case "овца" -> "Ты получаешь талисман овцы, пользуйся возможностью управлять астральной проекцией!";
            case "кролик" -> "Ты получаешь талисман кролика, теперь ты супер быстрый!";
            case "дракон" -> "Ты получаешь талисман дракона, у тебя взрывная сила!";
            case "крыса" -> "Ты получаешь талисман крысы, оживи неодушевлённое!";
            case "лошадь" -> "Ты получаешь талисман лошади, теперь ты исцеляешь раны!";
            case "собака" -> "Ты получаешь талисман собаки, получи бессмертие, вуфь!";
            case "свинья" -> "Ты получаешь талисман свиньи, теперь ты можешь стрелять пламенем из глаз!";
            case "обезьяна" -> "Ты получаешь талисман обезьяны, твоя особенность - превращение предметов и людей в животных!";
            case "тигр" -> "Ты получаешь талисман тигра, особая сила духовного равновесия между Инь и Ян!";
            default -> throw new IllegalStateException("Unexpected value: " + key);
        };
    }

}
