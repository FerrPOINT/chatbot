package azhukov.chatbot.controller;

import azhukov.chatbot.ChatbotApplication;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeansException;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SystemController implements ApplicationContextAware {

    private ApplicationContext context;

    @GetMapping("/exit")
    public void shutdown() {
        new Thread(() -> ((ConfigurableApplicationContext) context).close()).start();
    }

    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        this.context = ctx;
    }


}
