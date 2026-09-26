package com.back.springai.api;

import com.back.springai.domain.openai.entity.ChatEntity;
import com.back.springai.domain.openai.service.ChatService;
import com.back.springai.domain.openai.service.OpenAIService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ChatController {
    private final OpenAIService openAIService;
    private final ChatService chatService;



    @ResponseBody
    @PostMapping("/chat")
    public String chat(@RequestBody Map<String, String> body) {
        return openAIService.generate(body.get("text"));
    }

    @ResponseBody
    @PostMapping("/chat/stream")
    public Flux<String> streamChat(@RequestBody Map<String, String> body) {
        return openAIService.generateStream(body.get("text"));
    }


    @GetMapping("/")
    public String chatPage() {
        return "chat";
    }


    @ResponseBody
    @PostMapping("/chat/history/{userid}")
    public List<ChatEntity> getChatHistory(@PathVariable("userid") String userId) {
        return chatService.readAllChats(userId);
    }

}
