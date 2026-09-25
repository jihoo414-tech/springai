package com.back.springai.api;

import com.back.springai.domain.openai.service.OpenAIService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatController {
    private final OpenAIService openAIService;


}
