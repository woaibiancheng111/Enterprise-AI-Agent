package com.shixi.agent;

import com.shixi.app.EnterpriseApp;
import com.shixi.rag.model.SearchResult;
import com.shixi.rag.service.EnhancedRagService;
import com.shixi.rag.service.QueryRewriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 企业 AI 数字团队编排服务。
 *
 * 将现有 RAG、工具调用、工单生成能力包装成 KNOWLEDGE / RESPONDER /
 * ANALYZER / ROUTER / TRACKER 五个清晰的业务角色，便于前端展示完整处理链路。
 */
@Service
@Slf4j
public class DigitalTeamService {

    private final QueryRewriter queryRewriter;
    private final EnhancedRagService enhancedRagService;
    private final EnterpriseApp enterpriseApp;

    public DigitalTeamService(
            QueryRewriter queryRewriter,
            EnhancedRagService enhancedRagService,
            EnterpriseApp enterpriseApp) {
        this.queryRewriter = queryRewriter;
        this.enhancedRagService = enhancedRagService;
        this.enterpriseApp = enterpriseApp;
    }

    public DigitalTeamResponse process(String message, String chatId, int topK) {
        long start = System.currentTimeMillis();
        QueryRewriter.IntentType intentType = queryRewriter.recognizeIntent(message);
        String rewrittenQuery = queryRewriter.rewrite(message);
        SentimentSignal sentiment = analyzeSentiment(message);

        EnhancedRagService.RagResult ragResult = enhancedRagService.search(message, topK);
        RouteDecision routeDecision = route(message, intentType, sentiment, ragResult);

        String answer = generateAnswer(message, chatId, routeDecision);
        List<AgentStep> steps = buildSteps(message, intentType, rewrittenQuery, sentiment, ragResult, routeDecision, answer);
        TrackerInsight trackerInsight = buildTrackerInsight(message, sentiment, ragResult, routeDecision);

        long elapsed = System.currentTimeMillis() - start;
        log.info("数字团队处理完成: intent={}, risk={}, route={}, time={}ms",
                intentType, sentiment.riskLevel(), routeDecision.routeType(), elapsed);

        return new DigitalTeamResponse(
                true,
                chatId,
                message,
                rewrittenQuery,
                intentType.name(),
                sentiment,
                routeDecision,
                toCitationCards(ragResult),
                steps,
                trackerInsight,
                answer,
                elapsed,
                LocalDateTime.now().toString()
        );
    }

    private String generateAnswer(String message, String chatId, RouteDecision routeDecision) {
        if ("TOOL_WORKFLOW".equals(routeDecision.routeType())) {
            return enterpriseApp.doChatWithTools(message, chatId);
        }

        if ("HUMAN_ESCALATION".equals(routeDecision.routeType())) {
            String ragAnswer = enterpriseApp.doChatWithKnowledgeBase(message, chatId);
            return ragAnswer + "\n\n已识别为高优先级诉求，建议同步创建人工处理工单并安排负责人跟进。";
        }

        return enhancedRagService.chat(message, chatId, 5);
    }

    private RouteDecision route(
            String message,
            QueryRewriter.IntentType intentType,
            SentimentSignal sentiment,
            EnhancedRagService.RagResult ragResult) {
        String lowerMessage = message.toLowerCase(Locale.ROOT);
        boolean hasOperationalKeyword = containsAny(lowerMessage,
                "申请", "办理", "查询", "状态", "余额", "报销", "请假", "员工", "e001", "e002", "e003", "e004", "e005");

        if ("HIGH".equals(sentiment.riskLevel())) {
            return new RouteDecision(
                    "HUMAN_ESCALATION",
                    "P0",
                    "员工关系/行政负责人",
                    "情绪或投诉风险较高，需要人工快速介入并保留上下文。"
            );
        }

        if (intentType == QueryRewriter.IntentType.FEEDBACK) {
            return new RouteDecision(
                    "SERVICE_TICKET",
                    "P1",
                    "HR 服务台",
                    "反馈类问题适合生成服务工单，并在结案后进入满意度追踪。"
            );
        }

        if ((intentType == QueryRewriter.IntentType.APPLICATION || intentType == QueryRewriter.IntentType.STATUS_QUERY)
                && hasOperationalKeyword) {
            return new RouteDecision(
                    "TOOL_WORKFLOW",
                    "P2",
                    "MCP 工具执行",
                    "申请、查询类需求优先交给工具型 Agent 处理，缺少参数时由模型追问。"
            );
        }

        if (ragResult.getResults().isEmpty()) {
            return new RouteDecision(
                    "KNOWLEDGE_GAP",
                    "P2",
                    "知识库维护",
                    "知识库未召回有效内容，需要记录为知识缺口。"
            );
        }

        return new RouteDecision(
                "AUTO_RAG_RESPONSE",
                "P3",
                "自动知识库问答",
                "标准政策咨询由 RAG 自动答复，并附带引用来源。"
        );
    }

    private SentimentSignal analyzeSentiment(String message) {
        int score = 82;
        List<String> signals = new ArrayList<>();

        score = applySignal(message, signals, score, 30, "投诉", "举报", "严重", "无法接受", "太差", "离谱");
        score = applySignal(message, signals, score, 22, "生气", "愤怒", "不满", "失望", "催了", "没人处理");
        score = applySignal(message, signals, score, 18, "紧急", "马上", "立刻", "今天必须", "影响工作");
        score = applySignal(message, signals, score, 12, "怎么办", "为什么", "怎么还", "一直");

        if (signals.isEmpty()) {
            signals.add("未发现明显负面情绪信号");
        }

        String riskLevel;
        if (score < 45) {
            riskLevel = "HIGH";
        } else if (score < 68) {
            riskLevel = "MEDIUM";
        } else {
            riskLevel = "LOW";
        }

        return new SentimentSignal(score, riskLevel, signals);
    }

    private int applySignal(String message, List<String> signals, int score, int penalty, String... keywords) {
        for (String keyword : keywords) {
            if (message.contains(keyword)) {
                signals.add(keyword);
                return Math.max(0, score - penalty);
            }
        }
        return score;
    }

    private List<AgentStep> buildSteps(
            String message,
            QueryRewriter.IntentType intentType,
            String rewrittenQuery,
            SentimentSignal sentiment,
            EnhancedRagService.RagResult ragResult,
            RouteDecision routeDecision,
            String answer) {
        List<AgentStep> steps = new ArrayList<>();

        steps.add(new AgentStep(
                "knowledge",
                "KNOWLEDGE",
                "知识库管理员",
                "done",
                "完成查询改写和混合检索",
                "改写查询：" + rewrittenQuery + "；召回 " + ragResult.getResults().size() + " 条知识片段。"
        ));
        steps.add(new AgentStep(
                "responder",
                "RESPONDER",
                "智能响应员",
                "done",
                "识别意图并准备回复策略",
                "当前意图：" + intentType.name() + "；回复长度：" + safeLength(answer) + " 字。"
        ));
        steps.add(new AgentStep(
                "analyzer",
                "ANALYZER",
                "风险分析官",
                "done",
                "完成情绪与风险评分",
                "风险等级：" + sentiment.riskLevel() + "；稳定度评分：" + sentiment.score() + "。"
        ));
        steps.add(new AgentStep(
                "router",
                "ROUTER",
                "任务路由员",
                "done",
                "完成任务分派决策",
                routeDecision.priority() + " → " + routeDecision.assignee() + "；" + routeDecision.reason()
        ));
        steps.add(new AgentStep(
                "tracker",
                "TRACKER",
                "满意度追踪员",
                "watch",
                "生成后续追踪动作",
                buildTrackerSummary(message, sentiment, ragResult, routeDecision)
        ));

        return steps;
    }

    private TrackerInsight buildTrackerInsight(
            String message,
            SentimentSignal sentiment,
            EnhancedRagService.RagResult ragResult,
            RouteDecision routeDecision) {
        boolean knowledgeGap = ragResult.getResults().isEmpty() || "KNOWLEDGE_GAP".equals(routeDecision.routeType());
        List<String> actions = new ArrayList<>();

        if (knowledgeGap) {
            actions.add("记录未命中问题，建议补充知识库条目：" + message);
        }
        if ("HIGH".equals(sentiment.riskLevel()) || "MEDIUM".equals(sentiment.riskLevel())) {
            actions.add("结案后发送满意度评价，并对负面信号做归因分析。");
        }
        if ("SERVICE_TICKET".equals(routeDecision.routeType()) || "HUMAN_ESCALATION".equals(routeDecision.routeType())) {
            actions.add("追踪工单 SLA，超时前提醒负责人。");
        }
        if (actions.isEmpty()) {
            actions.add("对话完成后采集一键满意度评分，用于优化回答质量。");
        }

        return new TrackerInsight(knowledgeGap, actions);
    }

    private String buildTrackerSummary(
            String message,
            SentimentSignal sentiment,
            EnhancedRagService.RagResult ragResult,
            RouteDecision routeDecision) {
        if (ragResult.getResults().isEmpty()) {
            return "将该问题加入知识缺口池：" + message;
        }
        if ("HIGH".equals(sentiment.riskLevel())) {
            return "进入高风险闭环：人工介入、SLA 追踪、结案满意度回访。";
        }
        if ("TOOL_WORKFLOW".equals(routeDecision.routeType())) {
            return "跟踪申请或查询结果，必要时补发状态通知。";
        }
        return "记录回答质量与引用命中情况，等待用户评价。";
    }

    private List<CitationCard> toCitationCards(EnhancedRagService.RagResult ragResult) {
        return ragResult.getResults().stream()
                .limit(5)
                .map(this::toCitationCard)
                .toList();
    }

    private CitationCard toCitationCard(SearchResult result) {
        return new CitationCard(
                result.getSourceFile(),
                result.getSourceType(),
                result.getScore(),
                result.getHighlight(),
                result.getChunkIndex()
        );
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private int safeLength(String answer) {
        return answer == null ? 0 : answer.length();
    }

    public record DigitalTeamResponse(
            boolean success,
            String chatId,
            String query,
            String rewrittenQuery,
            String intentType,
            SentimentSignal sentiment,
            RouteDecision route,
            List<CitationCard> citations,
            List<AgentStep> steps,
            TrackerInsight tracker,
            String answer,
            long elapsedMs,
            String createdAt
    ) {
    }

    public record SentimentSignal(int score, String riskLevel, List<String> signals) {
    }

    public record RouteDecision(String routeType, String priority, String assignee, String reason) {
    }

    public record AgentStep(String code, String name, String title, String status, String summary, String output) {
    }

    public record CitationCard(String sourceFile, String sourceType, double score, String highlight, int chunkIndex) {
    }

    public record TrackerInsight(boolean knowledgeGap, List<String> actions) {
    }
}
