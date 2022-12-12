package azhukov.chatbot.service.arena;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class Fight {

    private final LocalDateTime localDateTime = LocalDateTime.now();
    private String firstUser;
    private String secondUser;

}
