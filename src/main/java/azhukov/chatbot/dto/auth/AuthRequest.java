package azhukov.chatbot.dto.auth;

import lombok.Data;

@Data
public class AuthRequest {

    private final String login;
    private final String password;

    public String getRequest() {
        return "login=" + login + "&password=" + password;
    }

}
