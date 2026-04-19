package com.shixi.controller;

import com.shixi.rag.model.SearchResult;
import com.shixi.rag.service.EnhancedRagService;
import com.shixi.rag.service.QueryRewriter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 增强版 RAG 控制器
 *
 * 提供高级 RAG 功能：
 * 1. 混合检索（向量 + BM25）
 * 2. 重排序
 * 3. 查询改写
 * 4. 引用溯源
 */
@RestController
@RequestMapping("rag")
@Tag(name = "增强版 RAG", description = "高级检索增强生成功能")
@Slf4j
public class EnhancedRagController {

    private final EnhancedRagService enhancedRagService;
    private final QueryRewriter queryRewriter;

    public EnhancedRagController(EnhancedRagService enhancedRagService, QueryRewriter queryRewriter) {
        this.enhancedRagService = enhancedRagService;
        this.queryRewriter = queryRewriter;
    }

    /**
     * 增强版 RAG 对话
     */
    @GetMapping("/chat")
    @Operation(summary = "增强版 RAG 对话", description = "使用混合检索 + 重排序 + 引用溯源进行对话")
    public ResponseEntity<Map<String, Object>> ragChat(
            @Parameter(description = "用户消息")
            @RequestParam String message,
            @Parameter(description = "会话ID")
            @RequestParam(defaultValue = "default-user") String chatId,
            @Parameter(description = "返回结果数量")
            @RequestParam(defaultValue = "5") int topK) {

        log.info("增强版 RAG 对话: message={}, chatId={}, topK={}", message, chatId, topK);

        String answer = enhancedRagService.chat(message, chatId, topK);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("answer", answer);
        result.put("chatId", chatId);

        return ResponseEntity.ok(result);
    }

    /**
     * 增强版 RAG 对话（流式）
     */
    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "增强版 RAG 对话（流式）", description = "流式返回 AI 回复")
    public Flux<ServerSentEvent<String>> ragChatStream(
            @Parameter(description = "用户消息")
            @RequestParam String message,
            @Parameter(description = "会话ID")
            @RequestParam(defaultValue = "default-user") String chatId,
            @Parameter(description = "返回结果数量")
            @RequestParam(defaultValue = "5") int topK) {

        log.info("增强版 RAG 流式对话: message={}, chatId={}, topK={}", message, chatId, topK);

        return enhancedRagService.streamChat(message, chatId, topK)
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

    /**
     * 仅检索（不生成回答）
     */
    @GetMapping("/search")
    @Operation(summary = "知识库检索", description = "仅执行混合检索，返回相关文档")
    public ResponseEntity<Map<String, Object>> search(
            @Parameter(description = "查询内容")
            @RequestParam String query,
            @Parameter(description = "返回结果数量")
            @RequestParam(defaultValue = "5") int topK) {

        log.info("知识库检索: query={}, topK={}", query, topK);

        EnhancedRagService.RagResult result = enhancedRagService.search(query, topK);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("query", result.getQuery());
        response.put("rewrittenQuery", result.getRewrittenQuery());
        response.put("intentType", result.getIntentType().name());
        response.put("searchStrategy", result.getSearchStrategy());
        response.put("retrievalTimeMs", result.getRetrievalTimeMs());
        response.put("results", result.getResults().stream()
                .map(this::toResultMap)
                .toList());

        return ResponseEntity.ok(response);
    }

    /**
     * 意图识别
     */
    @GetMapping("/intent")
    @Operation(summary = "查询意图识别", description = "识别用户查询的意图类型")
    public ResponseEntity<Map<String, Object>> recognizeIntent(
            @Parameter(description = "查询内容")
            @RequestParam String query) {

        QueryRewriter.IntentType intent = queryRewriter.recognizeIntent(query);
        String rewrittenQuery = queryRewriter.rewrite(query);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("originalQuery", query);
        result.put("rewrittenQuery", rewrittenQuery);
        result.put("intentType", intent.name());
        result.put("intentDescription", getIntentDescription(intent));

        return ResponseEntity.ok(result);
    }

    /**
     * 查询改写测试
     */
    @GetMapping("/rewrite")
    @Operation(summary = "查询改写", description = "将用户口语化查询转换为规范检索词")
    public ResponseEntity<Map<String, Object>> rewriteQuery(
            @Parameter(description = "原始查询")
            @RequestParam String query) {

        String rewritten = queryRewriter.rewrite(query);
        List<String> decomposed = queryRewriter.decompose(query);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("originalQuery", query);
        result.put("rewrittenQuery", rewritten);
        result.put("subQueries", decomposed);

        return ResponseEntity.ok(result);
    }

    /**
     * 将 SearchResult 转换为 Map
     */
    private Map<String, Object> toResultMap(SearchResult r) {
        Map<String, Object> map = new HashMap<>();
        map.put("content", r.getContent());
        map.put("sourceFile", r.getSourceFile());
        map.put("sourceType", r.getSourceType());
        map.put("score", r.getScore());
        map.put("chunkIndex", r.getChunkIndex());
        map.put("highlight", r.getHighlight());
        map.put("citation", r.toCitation());
        return map;
    }

    /**
     * 获取意图描述
     */
    private String getIntentDescription(QueryRewriter.IntentType intent) {
        return switch (intent) {
            case POLICY_QUERY -> "政策咨询类（查询规章制度、计算方式等）";
            case APPLICATION -> "申请办理类（申请假期、报销等）";
            case STATUS_QUERY -> "状态查询类（查询申请进度、余额等）";
            case CONSULTATION -> "咨询类（一般性问题）";
            case FEEDBACK -> "投诉建议类";
            case GENERAL -> "一般对话";
        };
    }
}
