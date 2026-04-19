package com.shixi.controller;

import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.shixi.app.EnterpriseApp;
import jakarta.annotation.Resource;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("enterprise")
public class EnterpriseController {

    @Resource
    private EnterpriseApp enterpriseApp;

    @GetMapping("/health")
    public String healthCheck() {
        return "Enterprise System OK: " + "\n";
    }

    @GetMapping("/chat")
    public String chat(@RequestParam String message, @RequestParam(defaultValue = "default-user") String chatId) {
        return enterpriseApp.doChat(message, chatId);
    }

    @GetMapping("/rag-chat")
    public String ragChat(@RequestParam String message, @RequestParam(defaultValue = "default-user") String chatId) {
        return enterpriseApp.doChatWithKnowledgeBase(message, chatId);
    }

    @GetMapping("/ticket")
    public Object generateTicket(@RequestParam String message, @RequestParam(defaultValue = "default-user") String chatId) {
        return enterpriseApp.doChatWithReport(message, chatId);
    }

    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatStream(
            @RequestParam String message,
            @RequestParam(defaultValue = "default-user") String chatId) {
        return enterpriseApp.streamChat(message, chatId)
                .map(content -> ServerSentEvent.<String>builder()
                        .id("1")
                        .event("chunk")
                        .data(content)
                        .build())
                .concatWith(Flux.just(
                        ServerSentEvent.<String>builder()
                                .id("end")
                                .event("done")
                                .data("[DONE]")
                                .build()
                ));
    }

    @GetMapping(value = "/rag-chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> ragChatStream(
            @RequestParam String message,
            @RequestParam(defaultValue = "default-user") String chatId,
            @RequestParam(defaultValue = "5") int topK) {
        return enterpriseApp.streamChatWithKnowledgeBaseWithCitations(message, chatId, topK)
                .map(content -> ServerSentEvent.<String>builder()
                        .id("1")
                        .event("chunk")
                        .data(content)
                        .build())
                .concatWith(Flux.just(
                        ServerSentEvent.<String>builder()
                                .id("end")
                                .event("done")
                                .data("[DONE]")
                                .build()
                ));
    }
}
