package azhukov.chatbot.constants;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

public enum MessageType {
    welcome,
    auth,
    success_auth,
    get_channels_list,
    channels_list,
    join,
    success_join,
    channel_counters,
    message,
    send_message,
    remove_message,
    accepted,
    ban2,
    user_ban,

    //
    ;
    private static final Map<String, MessageType> BY_NAME = Collections.unmodifiableMap(Arrays.stream(values()).collect(Collectors.toMap(Enum::name, mT -> mT)));

    public static MessageType byName(String name){
        return BY_NAME.get(name);
    }

}
