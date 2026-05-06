package com.shixi.controller;

import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.shixi.agent.DigitalTeamService;
import com.shixi.app.EnterpriseApp;
import com.shixi.security.ConversationIdResolver;
import jakarta.annotation.Resource;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("enterprise")
public class EnterpriseController {

    @Resource
    private EnterpriseApp enterpriseApp;

    @Resource
    private DigitalTeamService digitalTeamService;

    @Resource
    private ConversationIdResolver conversationIdResolver;

    @GetMapping("/health")
    public String healthCheck() {
        return "Enterprise System OK: " + "\n";
    }

    @GetMapping("/chat")
    public String chat(@RequestParam String message, @RequestParam(defaultValue = "default-user") String chatId) {
        return enterpriseApp.doChat(message, resolveChatId(chatId));
    }

    @PostMapping("/chat")
    public String chat(@RequestBody ChatRequest request) {
        return enterpriseApp.doChat(request.message(), resolveChatId(request.chatId()));
    }

    @GetMapping("/rag-chat")
    public String ragChat(@RequestParam String message, @RequestParam(defaultValue = "default-user") String chatId) {
        return enterpriseApp.doChatWithKnowledgeBase(message, resolveChatId(chatId));
    }

    @PostMapping("/rag-chat")
    public String ragChat(@RequestBody ChatRequest request) {
        return enterpriseApp.doChatWithKnowledgeBase(request.message(), resolveChatId(request.chatId()));
    }

    @GetMapping("/ticket")
    public Object generateTicket(@RequestParam String message, @RequestParam(defaultValue = "default-user") String chatId) {
        return enterpriseApp.doChatWithReport(message, resolveChatId(chatId));
    }

    @PostMapping("/ticket")
    public Object generateTicket(@RequestBody ChatRequest request) {
        return enterpriseApp.doChatWithReport(request.message(), resolveChatId(request.chatId()));
    }

    @GetMapping("/team-chat")
    public DigitalTeamService.DigitalTeamResponse teamChat(
            @RequestParam String message,
            @RequestParam(defaultValue = "default-user") String chatId,
            @RequestParam(defaultValue = "5") int topK) {
        return digitalTeamService.process(message, resolveChatId(chatId), topK);
    }

    @PostMapping("/team-chat")
    public DigitalTeamService.DigitalTeamResponse teamChat(@RequestBody ChatRequest request) {
        return digitalTeamService.process(request.message(), resolveChatId(request.chatId()), request.topKOrDefault());
    }

    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatStream(
            @RequestParam String message,
            @RequestParam(defaultValue = "default-user") String chatId) {
        return enterpriseApp.streamChat(message, resolveChatId(chatId))
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
        return enterpriseApp.streamChatWithKnowledgeBaseWithCitations(message, resolveChatId(chatId), topK)
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

    // ==================== MCP 工具对话接口 ====================

    @GetMapping("/tool-chat")
    public String toolChat(
            @RequestParam String message,
            @RequestParam(defaultValue = "default-user") String chatId) {
        return enterpriseApp.doChatWithTools(message, resolveChatId(chatId));
    }

    @PostMapping("/tool-chat")
    public String toolChat(@RequestBody ChatRequest request) {
        return enterpriseApp.doChatWithTools(request.message(), resolveChatId(request.chatId()));
    }

    @GetMapping(value = "/tool-chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> toolChatStream(
            @RequestParam String message,
            @RequestParam(defaultValue = "default-user") String chatId) {
        return enterpriseApp.streamChatWithTools(message, resolveChatId(chatId))
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

    private String resolveChatId(String chatId) {
        return conversationIdResolver.resolve(chatId);
    }

    public record ChatRequest(String message, String chatId, Integer topK) {

        public int topKOrDefault() {
            return topK == null || topK <= 0 ? 5 : topK;
        }
    }
}
