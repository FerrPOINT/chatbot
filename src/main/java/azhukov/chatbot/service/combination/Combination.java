package azhukov.chatbot.service.combination;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Combination {

    private String id;
    private List<String> commands;
    private List<String> staticParts;
    private List<List<String>> dynamicParts;

}
