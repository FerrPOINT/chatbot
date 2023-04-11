package azhukov.chatbot.service.combination;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class Combination {

    private final String id;
    private final List<String> commands;
    private final List<String> staticParts;
    private final List<List<String>> dynamicParts;

}
