package com.shixi.rag.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RAG 检索上下文
 * 用于传递给 AI 的上下文信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrievalContext {

    /**
     * 查询改写后的文本
     */
    private String rewrittenQuery;

    /**
     * 原始查询
     */
    private String originalQuery;

    /**
     * 检索到的文档块数量
     */
    private int retrievedCount;

    /**
     * 使用的检索策略
     */
    private String searchStrategy;

    /**
     * 检索耗时 (毫秒)
     */
    private long retrievalTimeMs;

    /**
     * 是否启用了重排序
     */
    private boolean reranked;

    /**
     * 生成上下文文本
     */
    public String toContextText() {
        StringBuilder sb = new StringBuilder();
        sb.append("【检索信息】\n");
        sb.append("- 策略: ").append(searchStrategy).append("\n");
        if (rewrittenQuery != null && !rewrittenQuery.equals(originalQuery)) {
            sb.append("- 查询改写: \"").append(originalQuery).append("\" -> \"").append(rewrittenQuery).append("\"\n");
        }
        sb.append("- 检索到 ").append(retrievedCount).append(" 个相关文档块\n");
        if (reranked) {
            sb.append("- 已进行结果重排序\n");
        }
        sb.append("- 耗时: ").append(retrievalTimeMs).append("ms\n");
        return sb.toString();
    }
}
