package com.back.springai.domain.openai.service;

import com.back.springai.domain.openai.entity.ChatEntity;
import com.back.springai.domain.openai.repository.ChatRepository;
import com.openai.models.audio.AudioResponseFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.audio.tts.TextToSpeechResponse;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.*;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OpenAIService {
    private final OpenAiChatModel openAiChatModel;
    private final OpenAiEmbeddingModel openAiEmbeddingModel;
    private final OpenAiImageModel openAiImageModel;
    private final OpenAiAudioSpeechModel openAiAudioSpeechModel;
    private final OpenAiAudioTranscriptionModel openAiAudioTranscriptionModel;
    private final ChatMemoryRepository chatMemoryRepository;
    private final ChatRepository chatRepository;




    //1.chatModel
    public String generate(String text){

        //메시지
        SystemMessage systemMessage = new SystemMessage("");
        UserMessage userMessage = new UserMessage(text);
        AssistantMessage assistantMessage = new AssistantMessage("");

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model("gpt-4.1-mini")
                .temperature(0.7)
                .build();


        Prompt prompt = new Prompt(List.of(systemMessage, userMessage, assistantMessage), options);

        ChatResponse response = openAiChatModel.call(prompt);


        return response.getResult().getOutput().getText();

    }

    public Flux<String> generateStream(String text) {

        String userId = "xxxjjhhh_3";

        ChatEntity userChat = new ChatEntity();
        userChat.setUserId(userId);
        userChat.setType(MessageType.USER);
        userChat.setContent(text);

        // 사용자 메시지는 바로 저장
        chatRepository.save(userChat);

        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .maxMessages(10)
                .chatMemoryRepository(chatMemoryRepository)
                .build();

        chatMemory.add(userId, new UserMessage(text));

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model("gpt-4.1-mini")
                .temperature(0.7)
                .build();

        Prompt prompt = new Prompt(
                chatMemory.get(userId),
                options
        );

        StringBuilder buffer = new StringBuilder();

        return openAiChatModel.stream(prompt)
                .mapNotNull(response -> {

                    String token = response.getResult()
                            .getOutput()
                            .getText();

                    if (token != null) {
                        buffer.append(token);
                        return token;
                    }

                    return null;
                })
                .doOnComplete(() -> {

                    String result = buffer.toString();

                    chatMemory.add(
                            userId,
                            new AssistantMessage(result)
                    );

                    ChatEntity assistantChat = new ChatEntity();
                    assistantChat.setUserId(userId);
                    assistantChat.setType(MessageType.ASSISTANT);
                    assistantChat.setContent(result);

                    chatRepository.save(assistantChat);
                });
    }



    //임베딩 api 호출 메서드
    public List<float[]> generateEmbedding(List<String> texts, String model) {

        // 옵션
        EmbeddingOptions embeddingOptions = OpenAiEmbeddingOptions.builder()
                .model(model).build();

        // 프롬프트
        EmbeddingRequest prompt = new EmbeddingRequest(texts, embeddingOptions);

        // 요청 및 응답
        EmbeddingResponse response = openAiEmbeddingModel.call(prompt);
        return response.getResults().stream()
                .map(Embedding::getOutput)
                .toList();
    }

    public List<String> generateImages(String text, int count, int height, int width) {

        // 옵션
        OpenAiImageOptions imageOptions = OpenAiImageOptions.builder()
                .quality("hd")
                .n(count)
                .height(height)
                .width(width)
                .build();

        // 프롬프트
        ImagePrompt prompt = new ImagePrompt(text, imageOptions);

        // 요청 및 응답
        ImageResponse response = openAiImageModel.call(prompt);
        return response.getResults().stream()
                .map(image -> image.getOutput().getUrl())
                .toList();
    }


    // TTS
    public byte[] tts(String text) {

        // 옵션
        OpenAiAudioSpeechOptions speechOptions = OpenAiAudioSpeechOptions.builder()
                .responseFormat(OpenAiAudioSpeechOptions.AudioResponseFormat.MP3)
                .speed(1.0)
                .model("tts-1")
                .build();

        // 프롬프트
        TextToSpeechPrompt prompt = new TextToSpeechPrompt(text, speechOptions);

        // 요청 및 응답
        TextToSpeechResponse response = openAiAudioSpeechModel.call(prompt);
        return response.getResult().getOutput();
    }

    // STT
    public String stt(Resource audioFile) {

        // 옵션
        OpenAiAudioTranscriptionOptions transcriptionOptions = OpenAiAudioTranscriptionOptions.builder()
                .language("ko") // 인식할 언어
                .prompt("Ask not this, but ask that") // 음성 인식 전 참고할 텍스트 프롬프트
                .temperature(0f)
                .model(OpenAiAudioTranscriptionOptions.DEFAULT_TRANSCRIPTION_MODEL)
                .responseFormat(AudioResponseFormat.VTT) // 결과 타입 지정 VTT 자막형식
                .build();

        // 프롬프트
        AudioTranscriptionPrompt prompt = new AudioTranscriptionPrompt(audioFile, transcriptionOptions);

        // 요청 및 응답
        AudioTranscriptionResponse response = openAiAudioTranscriptionModel.call(prompt);
        return response.getResult().getOutput();
    }

}

