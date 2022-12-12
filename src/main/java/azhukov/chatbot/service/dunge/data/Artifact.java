package azhukov.chatbot.service.dunge.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class Artifact {

    private final String name;
    private final String label;
    private final List<Modificator> modifications;

}
