package azhukov.chatbot.service.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class CommandsUtil {

    public static String getNextWordAfterCommand(String original, String lowerCase, String command) {
        String nextWordAfterCommand = getNextWordAfterCommand(lowerCase, command);
        if (nextWordAfterCommand == null) {
            return null;
        }
        int beginIndex = lowerCase.indexOf(nextWordAfterCommand);
        return original.substring(beginIndex, beginIndex + nextWordAfterCommand.length());
    }

    private static String getNextWordAfterCommand(String lowerCase, String command) {
        if (!lowerCase.contains(command)) {
            return null;
        }
        String[] tokenized = lowerCase.split(" ");
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
