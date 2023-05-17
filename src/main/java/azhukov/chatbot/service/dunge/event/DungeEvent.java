package azhukov.chatbot.service.dunge.event;

import azhukov.chatbot.service.dunge.data.HeroInfo;
import azhukov.chatbot.service.weight.WeightItem;

public interface DungeEvent extends WeightItem {

    String handle(HeroInfo hero);

}
