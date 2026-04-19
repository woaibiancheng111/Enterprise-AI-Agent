package com.shixi.rag.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 混合检索配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HybridSearchConfig {

    /**
     * 向量检索数量
     */
    @Builder.Default
    private int vectorTopK = 20;

    /**
     * 关键词检索数量
     */
    @Builder.Default
    private int keywordTopK = 20;

    /**
     * 最终返回数量
     */
    @Builder.Default
    private int finalTopK = 5;

    /**
     * RRF 的 k 参数 (通常设为 60)
     */
    @Builder.Default
    private double rrfK = 60.0;

    /**
     * 向量检索权重 (0-1)
     */
    @Builder.Default
    private double vectorWeight = 0.5;

    /**
     * 关键词检索权重 (0-1)
     */
    @Builder.Default
    private double keywordWeight = 0.5;

    /**
     * 是否启用重排序
     */
    @Builder.Default
    private boolean enableRerank = true;

    /**
     * 是否启用查询改写
     */
    @Builder.Default
    private boolean enableQueryRewrite = true;

    /**
     * 默认配置
     */
    public static HybridSearchConfig defaultConfig() {
        return HybridSearchConfig.builder().build();
    }
}
