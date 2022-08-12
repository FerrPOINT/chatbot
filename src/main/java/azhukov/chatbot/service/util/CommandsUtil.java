package azhukov.chatbot.service.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class CommandsUtil {

    public static String getNextWordAfterCommand(String message, String command) {
        if (!message.contains(command)) {
            return null;
        }
        String[] tokenized = message.split(" ");
        boolean found = false;
        for (String s : tokenized) {
            String trimmed = s.trim();
            if (!found) {
                found = trimmed.equals(command);
            } else if (!trimmed.isEmpty()) {
                return trimmed;
            }
        }
        return null;
    }

}
