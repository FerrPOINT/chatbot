package azhukov.chatbot.service.dunge.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Modificator {

    ModificationType modificationType;
    int value;

}
