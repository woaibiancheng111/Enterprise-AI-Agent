package com.shixi.rag.service;

import com.shixi.rag.model.HybridSearchConfig;
import com.shixi.rag.model.SearchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 混合检索服务
 *
 * 结合向量检索和 BM25 关键词检索，用 RRF 算法融合结果
 */
@Service
@Slf4j
public class HybridSearchService {

    private final VectorStore vectorStore;
    private final EmbeddingModel embeddingModel;
    private final ResourcePatternResolver resourcePatternResolver;

    @Value("${knowledge.base-path:src/main/resources/documents}")
    private String basePath;

    private final Map<String, List<DocumentContent>> documentCache = new ConcurrentHashMap<>();

    public HybridSearchService(VectorStore vectorStore, EmbeddingModel embeddingModel,
                              ResourcePatternResolver resourcePatternResolver) {
        this.vectorStore = vectorStore;
        this.embeddingModel = embeddingModel;
        this.resourcePatternResolver = resourcePatternResolver;
    }

    /**
     * 执行混合检索
     */
    public List<SearchResult> search(String query, HybridSearchConfig config) {
        long startTime = System.currentTimeMillis();

        String rewrittenQuery = query;
        if (config.isEnableQueryRewrite()) {
            rewrittenQuery = rewriteQuery(query);
        }

        // 1. 向量检索
        List<SearchResult> vectorResults = vectorSearch(rewrittenQuery, config.getVectorTopK());

        // 2. BM25 关键词检索
        List<SearchResult> keywordResults = keywordSearch(rewrittenQuery, config.getKeywordTopK());

        // 3. RRF 融合
        List<SearchResult> fusedResults = rrfFusion(vectorResults, keywordResults, config);

        // 4. 取前 finalTopK
        if (fusedResults.size() > config.getFinalTopK()) {
            fusedResults = fusedResults.subList(0, config.getFinalTopK());
        }

        long endTime = System.currentTimeMillis();
        log.info("混合检索完成: query={}, vectorResults={}, keywordResults={}, finalResults={}, time={}ms",
                query, vectorResults.size(), keywordResults.size(), fusedResults.size(), endTime - startTime);

        return fusedResults;
    }

    /**
     * 简单查询改写
     * 将用户口语化表达转为规范查询
     */
    private String rewriteQuery(String query) {
        // 简单的查询扩展 - 可以后续接入 LLM 进行更智能的改写
        List<String> expansions = new ArrayList<>();

        // 添加同义词扩展
        if (query.contains("请假")) {
            expansions.add("假期申请");
            expansions.add("休假制度");
        }
        if (query.contains("报销")) {
            expansions.add("费用报销");
            expansions.add("差旅报销");
        }
        if (query.contains("工资") || query.contains("薪资")) {
            expansions.add("薪酬福利");
        }
        if (query.contains("加班")) {
            expansions.add("加班制度");
            expansions.add("调休");
        }

        if (expansions.isEmpty()) {
            return query;
        }

        // 组合原始查询和扩展词
        return query + " " + String.join(" ", expansions);
    }

    /**
     * 向量检索
     */
    private List<SearchResult> vectorSearch(String query, int topK) {
        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .build();
        List<Document> documents = vectorStore.similaritySearch(searchRequest);
        return convertToSearchResults(documents, "vector");
    }

    /**
     * BM25 关键词检索
     */
    private List<SearchResult> keywordSearch(String query, int topK) {
        try {
            ensureDocumentCacheLoaded();

            String[] queryTerms = tokenize(query);
            if (queryTerms.length == 0) {
                return Collections.emptyList();
            }

            // 计算 BM25 分数
            Map<DocumentEntry, Double> scores = new HashMap<>();
            for (Map.Entry<String, List<DocumentContent>> entry : documentCache.entrySet()) {
                String fileName = entry.getKey();
                for (DocumentContent content : entry.getValue()) {
                    double score = calculateBM25(content.text(), queryTerms);
                    if (score > 0) {
                        scores.put(new DocumentEntry(fileName, content.index()), score);
                    }
                }
            }

            // 按分数排序
            return scores.entrySet().stream()
                    .sorted(Map.Entry.<DocumentEntry, Double>comparingByValue().reversed())
                    .limit(topK)
                    .map(e -> {
                        DocumentContent content = documentCache.get(e.getKey().fileName())
                                .get(e.getKey().index());
                        return SearchResult.builder()
                                .content(content.text())
                                .sourceFile(e.getKey().fileName())
                                .sourceType("bm25")
                                .score(e.getValue())
                                .chunkIndex(e.getKey().index())
                                .highlight(content.text().substring(0, Math.min(100, content.text().length())))
                                .build();
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.warn("BM25 检索失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * RRF (Reciprocal Rank Fusion) 融合
     */
    private List<SearchResult> rrfFusion(List<SearchResult> vectorResults,
                                         List<SearchResult> keywordResults,
                                         HybridSearchConfig config) {
        Map<String, SearchResult> resultMap = new LinkedHashMap<>();
        Map<String, Double> rrfScores = new HashMap<>();

        // 计算向量检索的 RRF 分数
        for (int i = 0; i < vectorResults.size(); i++) {
            SearchResult result = vectorResults.get(i);
            String key = getResultKey(result);
            double rrfScore = 1.0 / (config.getRrfK() + i + 1);
            rrfScores.merge(key, rrfScore * config.getVectorWeight(), Double::sum);
            resultMap.put(key, result);
        }

        // 计算关键词检索的 RRF 分数
        for (int i = 0; i < keywordResults.size(); i++) {
            SearchResult result = keywordResults.get(i);
            String key = getResultKey(result);
            double rrfScore = 1.0 / (config.getRrfK() + i + 1);
            rrfScores.merge(key, rrfScore * config.getKeywordWeight(), Double::sum);
            resultMap.putIfAbsent(key, result);
        }

        // 按 RRF 分数排序
        return rrfScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(e -> {
                    SearchResult result = resultMap.get(e.getKey());
                    result.setScore(e.getValue());
                    return result;
                })
                .collect(Collectors.toList());
    }

    /**
     * 将 Document 转换为 SearchResult
     */
    private List<SearchResult> convertToSearchResults(List<Document> documents, String sourceType) {
        List<SearchResult> results = new ArrayList<>();
        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            SearchResult result = SearchResult.fromDocument(doc, 1.0 - (i * 0.01), i);
            result.setSourceType(sourceType);
            results.add(result);
        }
        return results;
    }

    /**
     * 获取结果唯一标识
     */
    private String getResultKey(SearchResult result) {
        return result.getSourceFile() + ":" + result.getChunkIndex();
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
     * 计算 BM25 分数
     */
    private double calculateBM25(String document, String[] queryTerms) {
        String docLower = document.toLowerCase();
        double score = 0;
        double avgDocLen = 100; // 假设平均文档长度
        double k1 = 1.5;
        double b = 0.75;

        int docLen = tokenize(document).length;
        double docLenNorm = docLen / avgDocLen;

        for (String term : queryTerms) {
            if (docLower.contains(term)) {
                double tf = 1 + Math.log(1 + countTermFrequency(docLower, term));
                double idf = Math.log((documentCache.size() + 1) / 1.0);
                score += tf * idf / (k1 * (1 - b + b * docLenNorm) + tf);
            }
        }
        return score;
    }

    private int countTermFrequency(String text, String term) {
        return text.split(term, -1).length - 1;
    }

    /**
     * 确保文档缓存已加载
     */
    private void ensureDocumentCacheLoaded() {
        if (!documentCache.isEmpty()) {
            return;
        }

        try {
            // 加载 classpath 中的文档
            Resource[] resources = resourcePatternResolver.getResources("classpath:documents/*.md");
            for (Resource resource : resources) {
                String fileName = resource.getFilename();
                if (fileName == null) continue;

                List<DocumentContent> contents = new ArrayList<>();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder currentChunk = new StringBuilder();
                    String line;
                    int index = 0;

                    while ((line = reader.readLine()) != null) {
                        currentChunk.append(line).append("\n");
                        // 按段落切分
                        if (line.trim().isEmpty() && currentChunk.length() > 50) {
                            contents.add(new DocumentContent(currentChunk.toString().trim(), index++));
                            currentChunk = new StringBuilder();
                        }
                    }
                    if (currentChunk.length() > 0) {
                        contents.add(new DocumentContent(currentChunk.toString().trim(), index));
                    }
                }
                documentCache.put(fileName, contents);
            }

            log.info("BM25 索引加载完成: {} 个文件, {} 个文档块",
                    documentCache.size(),
                    documentCache.values().stream().mapToInt(List::size).sum());

        } catch (Exception e) {
            log.warn("加载 BM25 索引失败", e);
        }
    }

    /**
     * 清除缓存，强制重新加载
     */
    public void clearCache() {
        documentCache.clear();
        log.info("BM25 索引缓存已清除");
    }

    // 内部类：文档内容块
    private record DocumentContent(String text, int index) {}

    // 内部类：文档标识
    private record DocumentEntry(String fileName, int index) {}
}
