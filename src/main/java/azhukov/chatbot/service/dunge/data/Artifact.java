package azhukov.chatbot.service.dunge.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Artifact {

    private String id;
    private String name;
    private String label;
    private List<Modificator> modifications;

}
