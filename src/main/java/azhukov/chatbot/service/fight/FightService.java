package azhukov.chatbot.service.fight;

import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class FightService {

    private Fight fight = new Fight();
    private final Set<String> currentUsers = new HashSet<>();

    public synchronized Fight fight(String name) {
        if (currentUsers.contains(name)) {
            return null;
        }
        currentUsers.add(name);
        if (fight.getFirstUser() == null) {
            fight.setFirstUser(name);
        } else if (fight.getSecondUser() != null) {
            fight = new Fight();
            fight.setFirstUser(name);
        } else {
            fight.setSecondUser(name);
            currentUsers.remove(fight.getFirstUser());
            currentUsers.remove(fight.getSecondUser());
        }
        return fight;
    }


    //every 1 min
//    @Scheduled(cron = "0 */1 * ? * *")
//    void commit() {
//       reset outdated fights
//    }

}
