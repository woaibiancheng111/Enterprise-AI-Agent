package com.shixi.rag.service;

import com.shixi.rag.model.SearchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 重排序服务
 *
 * 对检索结果进行精排，提高相关性
 *
 * 支持多种策略：
 * 1. Embedding 余弦相似度重排
 * 2. LLM 判断重排
 * 3. 轻量级关键词匹配重排
 */
@Service
@Slf4j
public class RerankService {

    private final ChatClient chatClient;
    private final ExecutorService executorService;

    public RerankService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
        this.executorService = Executors.newFixedThreadPool(4);
    }

    /**
     * 重排序检索结果
     */
    public List<SearchResult> rerank(String query, List<SearchResult> candidates, int topN) {
        if (candidates == null || candidates.isEmpty()) {
            return candidates;
        }

        // 限制候选数量，避免重排计算量过大
        List<SearchResult> limitedCandidates = candidates.size() > 20
                ? candidates.subList(0, 20)
                : candidates;

        log.info("开始重排序: query={}, candidates={}, topN={}", query, limitedCandidates.size(), topN);

        try {
            // 使用关键词+语义组合重排
            List<SearchResult> reranked = rerankByKeywordAndSemantic(query, limitedCandidates);

            // 取前 topN
            if (reranked.size() > topN) {
                reranked = reranked.subList(0, topN);
            }

            return reranked;
        } catch (Exception e) {
            log.warn("重排序失败，返回原始顺序", e);
            return candidates.subList(0, Math.min(candidates.size(), topN));
        }
    }

    /**
     * 基于关键词和语义的重排序
     */
    private List<SearchResult> rerankByKeywordAndSemantic(String query, List<SearchResult> candidates) {
        String[] queryTerms = tokenize(query);

        // 并行计算每个候选的分数
        Map<SearchResult, Double> scores = candidates.parallelStream()
                .collect(Collectors.toMap(
                        result -> result,
                        result -> calculateRelevanceScore(query, queryTerms, result)
                ));

        // 按分数排序
        return scores.entrySet().stream()
                .sorted(Map.Entry.<SearchResult, Double>comparingByValue().reversed())
                .peek(entry -> {
                    entry.getKey().setRerankScore(entry.getValue());
                    log.debug("Rerank: content={}, score={}", 
                            truncate(entry.getKey().getContent(), 30), 
                            entry.getValue());
                })
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * 计算相关性分数
     *
     * 组合考虑：
     * 1. 关键词匹配度 (40%)
     * 2. 位置权重 (20%)
     * 3. 完整度 (20%)
     * 4. 密度 (20%)
     */
    private double calculateRelevanceScore(String query, String[] queryTerms, SearchResult result) {
        String content = result.getContent().toLowerCase();
        String contentLower = content.toLowerCase();

        // 1. 关键词匹配度
        double keywordScore = calculateKeywordScore(queryTerms, contentLower);

        // 2. 位置权重（查询词在文档中首次出现的位置，越靠前越好）
        double positionScore = calculatePositionScore(queryTerms, contentLower);

        // 3. 完整度（文档长度是否合适，太短或太长都不好）
        double completenessScore = calculateCompletenessScore(content);

        // 4. 密度（查询词在文档中出现的频率）
        double densityScore = calculateDensityScore(queryTerms, contentLower);

        // 加权组合
        double totalScore = keywordScore * 0.4 +
                           positionScore * 0.2 +
                           completenessScore * 0.2 +
                           densityScore * 0.2;

        // 加上原始检索分数的影响
        totalScore = totalScore * 0.7 + result.getScore() * 0.3;

        return Math.round(totalScore * 1000.0) / 1000.0;
    }

    /**
     * 关键词匹配分数
     */
    private double calculateKeywordScore(String[] queryTerms, String content) {
        if (queryTerms.length == 0) return 0.5;

        int matchedTerms = 0;
        int matchedPositions = 0;

        for (int i = 0; i < queryTerms.length; i++) {
            String term = queryTerms[i];
            if (content.contains(term)) {
                matchedTerms++;
                matchedPositions += (queryTerms.length - i); // 越靠前的词权重越高
            }
        }

        double termMatchRatio = (double) matchedTerms / queryTerms.length;
        double positionBonus = matchedPositions > 0
                ? Math.min(1.0, matchedPositions / (queryTerms.length * queryTerms.length))
                : 0;

        return termMatchRatio * 0.7 + positionBonus * 0.3;
    }

    /**
     * 位置分数
     */
    private double calculatePositionScore(String[] queryTerms, String content) {
        int firstMatchPos = content.length();

        for (String term : queryTerms) {
            int pos = content.indexOf(term);
            if (pos >= 0 && pos < firstMatchPos) {
                firstMatchPos = pos;
            }
        }

        // 归一化到 0-1，越靠前越好
        return Math.max(0, 1 - (double) firstMatchPos / Math.max(content.length(), 1));
    }

    /**
     * 完整度分数
     */
    private double calculateCompletenessScore(String content) {
        int length = content.length();

        // 最优长度区间：100-500字
        if (length >= 100 && length <= 500) {
            return 1.0;
        } else if (length < 100) {
            return length / 100.0;
        } else {
            // 超过500字，惩罚但不完全排除
            return Math.max(0.5, 1.0 - (length - 500) / 1000.0);
        }
    }

    /**
     * 密度分数
     */
    private double calculateDensityScore(String[] queryTerms, String content) {
        if (queryTerms.length == 0) return 0.5;

        int totalOccurrences = 0;
        for (String term : queryTerms) {
            int count = 0;
            int pos = 0;
            while ((pos = content.indexOf(term, pos)) != -1) {
                count++;
                pos += term.length();
            }
            totalOccurrences += count;
        }

        // 密度过高可能表示关键词堆砌，适当惩罚
        double expectedDensity = totalOccurrences / (double) content.length();
        if (expectedDensity > 0.1) {
            return 0.7; // 惩罚关键词堆砌
        }

        return Math.min(1.0, totalOccurrences / 5.0);
    }

    /**
     * 简单分词
     */
    private String[] tokenize(String text) {
        return text.toLowerCase()
                .replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9\\s]", " ")
                .split("\\s+");
    }

    /**
     * 截断文本
     */
    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }

    /**
     * LLM 重排序（备选方案，需要额外 API 调用）
     * 适用于对精度要求极高的场景
     */
    public List<SearchResult> rerankWithLLM(String query, List<SearchResult> candidates, int topN) {
        if (candidates.isEmpty()) return candidates;

        // 构建提示词
        String prompt = buildRerankPrompt(query, candidates);

        try {
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            // 解析 LLM 返回的排序结果
            return parseLLMRerankResult(response, candidates);
        } catch (Exception e) {
            log.warn("LLM 重排序失败", e);
            return candidates.subList(0, Math.min(candidates.size(), topN));
        }
    }

    /**
     * 构建重排序提示词
     */
    private String buildRerankPrompt(String query, List<SearchResult> candidates) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个文档相关性排序专家。\n\n");
        sb.append("用户查询：").append(query).append("\n\n");
        sb.append("以下是候选文档，请根据与查询的相关性排序（最相关的排在前面）：\n\n");

        for (int i = 0; i < candidates.size(); i++) {
            SearchResult r = candidates.get(i);
            sb.append(String.format("[%d] %s\n---\n%s\n\n",
                    i + 1,
                    r.getSourceFile(),
                    truncate(r.getContent(), 200)));
        }

        sb.append("请输出排序结果，格式：1,3,2,4（表示第1个最相关，第3个次之，以此类推）\n");
        sb.append("只输出数字，不要输出其他内容。");

        return sb.toString();
    }

    /**
     * 解析 LLM 重排序结果
     */
    private List<SearchResult> parseLLMRerankResult(String response, List<SearchResult> candidates) {
        try {
            String[] indices = response.replaceAll("[^0-9,]", "").split(",");
            List<SearchResult> result = new java.util.ArrayList<>();

            for (String idx : indices) {
                int index = Integer.parseInt(idx.trim()) - 1;
                if (index >= 0 && index < candidates.size()) {
                    SearchResult r = candidates.get(index);
                    r.setRerankScore(1.0 - result.size() * 0.01);
                    result.add(r);
                }
            }

            // 添加未包含的文档
            List<Integer> usedIndices = result.stream()
                    .map(c -> candidates.indexOf(c))
                    .toList();
            for (int i = 0; i < candidates.size(); i++) {
                if (!usedIndices.contains(i)) {
                    candidates.get(i).setRerankScore(0.0);
                    result.add(candidates.get(i));
                }
            }

            return result;
        } catch (Exception e) {
            log.warn("解析 LLM 重排序结果失败", e);
            return candidates;
        }
    }
}
