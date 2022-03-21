package azhukov.chatbot.service.variety;

import lombok.Data;

import java.util.List;

@Data
public class VarietyList {

    private String id;
    private String name;
    private List<String> commands;
    private List<Variety> varieties;

}
