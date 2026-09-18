package azhukov.chatbot.service.dunge.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OwnedArtifact {
    private String id;
    private int rank = 1;

    public OwnedArtifact(String id) {
        this(id, 1);
    }

    public void normalize() {
        rank = Math.max(1, Math.min(5, rank));
    }
}
