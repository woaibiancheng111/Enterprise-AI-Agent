package com.shixi.rag.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;

/**
 * RAG 搜索结果模型
 *
 * 包含文档内容和元数据，支持引用溯源
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult {

    /**
     * 文档内容块
     */
    private String content;

    /**
     * 来源文件名
     */
    private String sourceFile;

    /**
     * 来源类型: classpath / upload
     */
    private String sourceType;

    /**
     * 相似度分数 (0-1)
     */
    private double score;

    /**
     * 文档块 ID (用于溯源)
     */
    private String chunkId;

    /**
     * 文档索引 (在原文件中的位置)
     */
    private int chunkIndex;

    /**
     * 原始文档对象 (用于传递更多元数据)
     */
    private Document sourceDocument;

    /**
     * 排序分数 (Rerank 后)
     */
    private double rerankScore;

    /**
     * 引用文本 (高亮片段)
     */
    private String highlight;

    /**
     * 创建 SearchResult 从 Document
     */
    public static SearchResult fromDocument(Document doc, double score, int index) {
        Map<String, Object> metadata = doc.getMetadata();

        return SearchResult.builder()
                .content(doc.getText())
                .sourceFile(getMetadataString(metadata, "filename", "unknown"))
                .sourceType(getMetadataString(metadata, "source", "unknown"))
                .score(score)
                .chunkId(doc.getId())
                .chunkIndex(index)
                .sourceDocument(doc)
                .rerankScore(0.0)
                .highlight(doc.getText().substring(0, Math.min(100, doc.getText().length())))
                .build();
    }

    /**
     * 格式化引用文本
     */
    public String toCitation() {
        return String.format("[%s] %s (相关度: %.2f%%)",
                sourceFile,
                highlight.length() > 50 ? highlight.substring(0, 50) + "..." : highlight,
                score * 100);
    }

    /**
     * 获取完整的溯源信息
     */
    public String toSourceInfo() {
        return String.format("来源: %s | 文件: %s | 块: #%d | 相关度: %.2f%%",
                sourceType, sourceFile, chunkIndex + 1, score * 100);
    }

    private static String getMetadataString(Map<String, Object> metadata, String key, String defaultValue) {
        if (metadata == null) return defaultValue;
        Object value = metadata.get(key);
        return value != null ? value.toString() : defaultValue;
    }
}
