package azhukov.chatbot.service.webclient;

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;

@RequiredArgsConstructor
//@Service
public class TwitchWebClient {

    private final TwitchProperties properties;

    @PostConstruct
    public void init() {
        TwitchClient twitchClient = TwitchClientBuilder.builder()
                .withDefaultAuthToken(new OAuth2Credential("twitch", properties.token))
                .withEnableChat(true)
                .build();


        twitchClient.getChat().joinChannel(properties.channel);
        System.out.println();
    }

    public static void main1(String[] args) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
//        map.add("client_id",properties.token);
        map.add("client_secret", "");
        map.add("grant_type", "client_credentials");

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(map, headers);

        ResponseEntity<String> response =
                restTemplate.exchange("https://id.twitch.tv/oauth2/token",
                        HttpMethod.POST,
                        entity,
                        String.class);
        System.out.println("body = " + response);
    }

    @Component
    @ConfigurationProperties("twitch")
    public static class TwitchProperties {
        String channel;
        String token;
    }

}
