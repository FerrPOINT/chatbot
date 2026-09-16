package azhukov.chatbot.service.dunge;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class DungeonTime {
    public LocalDateTime now() { return LocalDateTime.now(); }
    /** Dungeon day changes at 03:00 application time. */
    public LocalDate today() { return now().minusHours(3).toLocalDate(); }
}
