package azhukov.chatbot.service.dunge.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StolenArtifact {
    private String id;
    private int rank;
    private String source;
    private LocalDateTime stolenAt;
}
