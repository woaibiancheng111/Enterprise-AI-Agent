package com.shixi.rag.service;

import com.shixi.rag.model.HybridSearchConfig;
import com.shixi.rag.model.SearchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 增强版 RAG 服务
 *
 * 集成混合检索、重排序、查询改写、引用溯源
 */
@Service
@Slf4j
public class EnhancedRagService {

    private final HybridSearchService hybridSearchService;
    private final RerankService rerankService;
    private final QueryRewriter queryRewriter;
    private final ChatClient chatClient;

    @Value("${rag.hybrid-search.final-top-k:5}")
    private int defaultTopK;

    @Value("${rag.hybrid-search.enable-rerank:true}")
    private boolean enableRerank;

    @Value("${rag.hybrid-search.enable-query-rewrite:true}")
    private boolean enableQueryRewrite;

    public EnhancedRagService(
            HybridSearchService hybridSearchService,
            RerankService rerankService,
            QueryRewriter queryRewriter,
            ChatClient.Builder chatClientBuilder) {
        this.hybridSearchService = hybridSearchService;
        this.rerankService = rerankService;
        this.queryRewriter = queryRewriter;
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * RAG 检索
     *
     * @param query 用户查询
     * @return 检索结果列表
     */
    public RagResult search(String query) {
        return search(query, defaultTopK);
    }

    /**
     * RAG 检索
     *
     * @param query 用户查询
     * @param topK 返回结果数量
     * @return 检索结果
     */
    public RagResult search(String query, int topK) {
        long startTime = System.currentTimeMillis();

        // 1. 查询改写
        String rewrittenQuery = enableQueryRewrite
                ? queryRewriter.rewrite(query)
                : query;

        // 2. 意图识别
        QueryRewriter.IntentType intentType = queryRewriter.recognizeIntent(query);

        // 3. 混合检索配置
        HybridSearchConfig config = HybridSearchConfig.builder()
                .vectorTopK(20)
                .keywordTopK(20)
                .finalTopK(Math.max(topK * 2, 10)) // 先检索更多，再重排
                .enableRerank(enableRerank)
                .enableQueryRewrite(false) // 已经改写过了
                .build();

        // 4. 执行混合检索
        List<SearchResult> results = hybridSearchService.search(rewrittenQuery, config);

        // 5. 重排序
        if (enableRerank && !results.isEmpty()) {
            results = rerankService.rerank(rewrittenQuery, results, topK);
        }

        // 6. 取最终 topK
        if (results.size() > topK) {
            results = results.subList(0, topK);
        }

        long endTime = System.currentTimeMillis();

        log.info("RAG 检索完成: query={}, rewritten={}, intent={}, results={}, time={}ms",
                query, rewrittenQuery, intentType, results.size(), endTime - startTime);

        return RagResult.builder()
                .query(query)
                .rewrittenQuery(rewrittenQuery)
                .intentType(intentType)
                .results(results)
                .retrievalTimeMs(endTime - startTime)
                .searchStrategy(enableRerank ? "hybrid+rerank" : "hybrid")
                .build();
    }

    /**
     * RAG 对话（单次）
     */
    public String chat(String query, String chatId) {
        return chat(query, chatId, defaultTopK);
    }

    /**
     * RAG 对话
     */
    public String chat(String query, String chatId, int topK) {
        // 1. 执行 RAG 检索
        RagResult ragResult = search(query, topK);

        // 2. 构建上下文
        String context = buildContext(ragResult);

        // 3. 调用 AI（直接传入上下文，不需要 QuestionAnswerAdvisor）
        ChatResponse response = chatClient.prompt()
                .user(buildUserMessage(query, context))
                .call()
                .chatResponse();

        String answer = response != null ? response.getResult().getOutput().getText() : "";

        // 4. 构建带引用的回答
        return buildAnswerWithCitations(answer, ragResult);
    }

    /**
     * RAG 对话（流式）
     */
    public Flux<String> streamChat(String query, String chatId) {
        return streamChat(query, chatId, defaultTopK);
    }

    /**
     * RAG 对话（流式）
     */
    public Flux<String> streamChat(String query, String chatId, int topK) {
        // 1. 执行 RAG 检索
        RagResult ragResult = search(query, topK);

        // 2. 构建上下文
        String context = buildContext(ragResult);

        // 3. 先返回引用信息
        String citations = buildCitationsText(ragResult);

        // 4. 流式调用 AI
        Flux<String> aiStream = chatClient.prompt()
                .user(buildUserMessage(query, context))
                .stream()
                .content();

        // 5. 合并引用信息和 AI 回复
        return Flux.just(citations + "\n\n")
                .concatWith(aiStream);
    }

    /**
     * 构建用户消息
     */
    private String buildUserMessage(String query, String context) {
        return "请根据以下背景信息回答用户的问题。\n\n" +
                "【背景信息】\n" + context + "\n\n" +
                "【用户问题】\n" + query;
    }

    /**
     * 构建上下文文本
     */
    private String buildContext(RagResult ragResult) {
        StringBuilder sb = new StringBuilder();

        // 检索信息
        sb.append("【检索信息】\n");
        sb.append("- 策略: ").append(ragResult.getSearchStrategy()).append("\n");
        if (!ragResult.getQuery().equals(ragResult.getRewrittenQuery())) {
            sb.append("- 查询改写: \"").append(ragResult.getQuery())
                    .append("\" -> \"").append(ragResult.getRewrittenQuery()).append("\"\n");
        }
        sb.append("- 检索到 ").append(ragResult.getResults().size()).append(" 个相关文档\n");
        sb.append("- 耗时: ").append(ragResult.getRetrievalTimeMs()).append("ms\n\n");

        // 文档内容
        sb.append("【相关文档】\n");
        for (int i = 0; i < ragResult.getResults().size(); i++) {
            SearchResult result = ragResult.getResults().get(i);
            sb.append(String.format("【文档 %d】 来源: %s\n%s\n\n",
                    i + 1,
                    result.getSourceFile(),
                    result.getContent()));
        }

        return sb.toString();
    }

    /**
     * 构建带引用的回答
     */
    private String buildAnswerWithCitations(String answer, RagResult ragResult) {
        if (ragResult.getResults().isEmpty()) {
            return answer;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(answer);
        sb.append("\n\n---\n");
        sb.append("【参考来源】\n");

        for (int i = 0; i < ragResult.getResults().size(); i++) {
            SearchResult result = ragResult.getResults().get(i);
            sb.append(String.format("%d. [%s] %s (相关度: %.1f%%)\n",
                    i + 1,
                    result.getSourceFile(),
                    result.getHighlight(),
                    result.getScore() * 100));
        }

        return sb.toString();
    }

    /**
     * 构建引用文本（用于流式输出）
     */
    private String buildCitationsText(RagResult ragResult) {
        if (ragResult.getResults().isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("【找到 ").append(ragResult.getResults().size()).append(" 条相关文档】\n");

        for (int i = 0; i < Math.min(ragResult.getResults().size(), 3); i++) {
            SearchResult result = ragResult.getResults().get(i);
            sb.append(String.format("[%d] %s - %s\n",
                    i + 1,
                    result.getSourceFile(),
                    truncate(result.getHighlight(), 60)));
        }

        return sb.toString();
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }

    /**
     * RAG 结果封装
     */
    @lombok.Data
    @lombok.Builder
    public static class RagResult {
        private String query;
        private String rewrittenQuery;
        private QueryRewriter.IntentType intentType;
        private List<SearchResult> results;
        private long retrievalTimeMs;
        private String searchStrategy;
    }
}
