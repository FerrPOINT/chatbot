package azhukov.chatbot.service.auth;

import azhukov.chatbot.dto.auth.AuthRequest;
import azhukov.chatbot.dto.auth.AuthResponse;
import azhukov.chatbot.dto.gg.GgChatRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

@Service
@Slf4j
@RequiredArgsConstructor
public class GgAuthService {

    @Getter
    @Value("${auth.login}")
    private String login;
    @Value("${auth.password}")
    private String password;

    private AuthResponse currentAuthData;

    public AuthResponse getAuthData() {
        if (currentAuthData == null) {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<String> entity = new HttpEntity<String>(new AuthRequest(login, password).getRequest() ,headers);

            ResponseEntity<AuthResponse> resp = restTemplate.postForEntity(
                    URI.create("https://goodgame.ru/ajax/chatlogin"),
                    entity,
                    AuthResponse.class
            );
            if (resp.getStatusCode() != HttpStatus.OK) {
                throw new IllegalStateException("Can't login, status: " + resp.getStatusCode());
            }
            currentAuthData = resp.getBody();
        }
        return currentAuthData;
    }

    public boolean isCurrentUser(GgChatRequest message) {
        return login.equals(message.getUserName());
    }

}
