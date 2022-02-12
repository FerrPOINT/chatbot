package azhukov.chatbot.controller;

import azhukov.chatbot.service.pet.LifecycleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class StatController {

    private final LifecycleService lifecycleService;

    @GetMapping("/life")
    public String index() {
        return lifecycleService.current().getMessage();
    }

}
