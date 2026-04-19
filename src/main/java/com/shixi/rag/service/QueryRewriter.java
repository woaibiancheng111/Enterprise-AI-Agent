package com.shixi.rag.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 查询改写服务
 *
 * 将用户口语化表达转换为规范的检索查询
 *
 * 支持的改写策略：
 * 1. 同义词扩展
 * 2. 查询纠错
 * 3. 查询分解
 * 4. 意图识别
 */
@Service
@Slf4j
public class QueryRewriter {

    /**
     * 同义词映射表（企业 HR 领域）
     */
    private static final Map<String, List<String>> SYNONYMS = Map.ofEntries(
            Map.entry("请假", List.of("假期", "休假", "离开", "休息")),
            Map.entry("年假", List.of("带薪年假", "年度假期", "法定年假")),
            Map.entry("病假", List.of("病休", "疾病假", "医疗期")),
            Map.entry("事假", List.of("私事假", "个人事假")),
            Map.entry("报销", List.of("费用", "票据", "发票", "补贴")),
            Map.entry("差旅", List.of("出差", "外出", "公务旅行")),
            Map.entry("工资", List.of("薪资", "薪酬", "收入", "报酬")),
            Map.entry("福利", List.of("待遇", "补贴", "保险", "社保")),
            Map.entry("加班", List.of("超时工作", "额外工作时间")),
            Map.entry("调休", List.of("补休", "换休")),
            Map.entry("打卡", List.of("考勤", "签到", "签退")),
            Map.entry("离职", List.of("辞职", "离开公司", "终止合同")),
            Map.entry("入职", List.of("报到", "开始工作", "新员工")),
            Map.entry("转正", List.of("正式员工", "试用期结束")),
            Map.entry("社保", List.of("社会保险", "五险", "公积金")),
            Map.entry("体检", List.of("健康检查", "年度体检")),
            Map.entry("培训", List.of("学习", "课程", "技能提升")),
            Map.entry("绩效", List.of("考核", "评估", "KPI")),
            Map.entry("奖金", List.of("年终奖", "绩效奖金", "分红")),
            Map.entry("补贴", List.of("津贴", "补助", "福利金"))
    );

    /**
     * 常见查询错误纠正
     */
    private static final Map<String, String> QUERY_CORRECTIONS = Map.ofEntries(
            Map.entry("销报", "报销"),
            Map.entry("年家", "年假"),
            Map.entry("并假", "病假"),
            Map.entry("旷工", "请假"),
            Map.entry("薪资", "工资"),
            Map.entry("工资", "薪资"),
            Map.entry("加班费", "加班补贴")
    );

    /**
     * 停用词列表（检索时不重要的词）
     */
    private static final Set<String> STOP_WORDS = Set.of(
            "的", "了", "是", "在", "有", "和", "与", "或", "地", "得",
            "我", "你", "他", "她", "它", "我们", "你们", "他们",
            "这个", "那个", "什么", "怎么", "如何", "多少",
            "请问", "想问", "想问一下", "问一下", "帮忙", "帮助"
    );

    /**
     * 改写查询
     */
    public String rewrite(String query) {
        if (query == null || query.isBlank()) {
            return query;
        }

        log.debug("开始改写查询: {}", query);

        // 1. 纠错
        String corrected = correctQuery(query);
        if (!corrected.equals(query)) {
            log.debug("查询纠错: {} -> {}", query, corrected);
        }

        // 2. 去除停用词
        String filtered = filterStopWords(corrected);

        // 3. 同义词扩展
        String expanded = expandSynonyms(filtered);

        // 4. 清理多余空格
        String cleaned = cleanQuery(expanded);

        log.debug("查询改写完成: {} -> {}", query, cleaned);
        return cleaned;
    }

    /**
     * 分解复杂查询为多个子查询
     */
    public List<String> decompose(String query) {
        List<String> subQueries = new ArrayList<>();

        // 按标点符号分解
        String[] parts = query.split("[，,。.;；!！?？]");

        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty() && trimmed.length() > 2) {
                subQueries.add(rewrite(trimmed));
            }
        }

        // 如果分解后为空，返回原查询
        if (subQueries.isEmpty()) {
            subQueries.add(rewrite(query));
        }

        return subQueries;
    }

    /**
     * 识别查询意图类型
     */
    public IntentType recognizeIntent(String query) {
        String lowerQuery = query.toLowerCase();

        // 政策咨询类
        if (containsAny(lowerQuery, "怎么", "如何", "什么", "多少", "几天", "多久", "规定", "政策", "制度")) {
            return IntentType.POLICY_QUERY;
        }

        // 申请办理类
        if (containsAny(lowerQuery, "申请", "办理", "办理", "流程", "步骤", "需要", "准备", "材料")) {
            return IntentType.APPLICATION;
        }

        // 状态查询类
        if (containsAny(lowerQuery, "查询", "查看", "我的", "状态", "进度", "情况")) {
            return IntentType.STATUS_QUERY;
        }

        // 咨询类
        if (containsAny(lowerQuery, "请问", "咨询", "问", "了解")) {
            return IntentType.CONSULTATION;
        }

        // 投诉建议类
        if (containsAny(lowerQuery, "投诉", "建议", "反馈", "问题", "不满")) {
            return IntentType.FEEDBACK;
        }

        return IntentType.GENERAL;
    }

    /**
     * 查询纠错
     */
    private String correctQuery(String query) {
        String corrected = query;

        for (Map.Entry<String, String> entry : QUERY_CORRECTIONS.entrySet()) {
            if (corrected.contains(entry.getKey())) {
                corrected = corrected.replace(entry.getKey(), entry.getValue());
            }
        }

        return corrected;
    }

    /**
     * 去除停用词
     */
    private String filterStopWords(String query) {
        StringBuilder result = new StringBuilder();

        for (String word : query.split("\\s+")) {
            if (!STOP_WORDS.contains(word) && word.length() > 1) {
                result.append(word).append(" ");
            }
        }

        return result.toString().trim();
    }

    /**
     * 同义词扩展
     */
    private String expandSynonyms(String query) {
        Set<String> terms = new LinkedHashSet<>();

        // 保留原始查询词
        for (String word : query.split("\\s+")) {
            terms.add(word);

            // 添加同义词
            for (Map.Entry<String, List<String>> entry : SYNONYMS.entrySet()) {
                if (word.contains(entry.getKey()) || entry.getKey().contains(word)) {
                    terms.addAll(entry.getValue());
                }
            }
        }

        return String.join(" ", terms);
    }

    /**
     * 清理查询
     */
    private String cleanQuery(String query) {
        return query
                .replaceAll("\\s+", " ")
                .replaceAll("^[\\s,，]+", "")
                .replaceAll("[\\s,，]+$", "")
                .trim();
    }

    /**
     * 检查是否包含任意关键词
     */
    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 意图类型枚举
     */
    public enum IntentType {
        /**
         * 政策查询（如：年假是怎么计算的？）
         */
        POLICY_QUERY,

        /**
         * 申请办理（如：我想申请年假，需要什么材料？）
         */
        APPLICATION,

        /**
         * 状态查询（如：我的请假申请到哪一步了？）
         */
        STATUS_QUERY,

        /**
         * 咨询类
         */
        CONSULTATION,

        /**
         * 投诉建议
         */
        FEEDBACK,

        /**
         * 一般对话
         */
        GENERAL
    }
}
