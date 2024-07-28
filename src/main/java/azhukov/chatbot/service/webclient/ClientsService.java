package azhukov.chatbot.service.webclient;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ClientsService {

    private final GgWebClient ggWebClient;
    private final DiscordWebClient discordWebClient;

    //every day 6:00
    @Scheduled(cron = "0 0 6 * * ?")
    synchronized void forceReconnect() {
        try {
            ggWebClient.forceReconnect();
        } finally {
            discordWebClient.forceReconnect();
        }
    }

}
