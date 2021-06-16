package azhukov.chatbot.service.auth;

import azhukov.chatbot.dto.auth.AuthRequest;
import azhukov.chatbot.dto.auth.AuthResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

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

}
