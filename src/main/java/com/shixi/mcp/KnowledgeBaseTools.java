package com.shixi.mcp;

import com.shixi.rag.model.SearchResult;
import com.shixi.rag.service.EnhancedRagService;
import lombok.Data;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class KnowledgeBaseTools {

    private final EnhancedRagService enhancedRagService;

    public KnowledgeBaseTools(EnhancedRagService enhancedRagService) {
        this.enhancedRagService = enhancedRagService;
    }

    @Tool(description = "检索企业知识库中的相关文档，根据用户查询返回最相关的文档内容")
    public KnowledgeSearchResult searchKnowledgeBase(
            @ToolParam(description = "用户的查询内容", required = true) String query,
            @ToolParam(description = "返回的最大文档数量，默认5条", required = false) Integer topK) {
        
        int actualTopK = (topK != null && topK > 0) ? topK : 5;
        
        EnhancedRagService.RagResult ragResult = enhancedRagService.search(query, actualTopK);
        
        List<DocumentInfo> documents = ragResult.getResults().stream()
                .map(this::toDocumentInfo)
                .collect(Collectors.toList());
        
        return new KnowledgeSearchResult(
                query,
                ragResult.getRewrittenQuery(),
                ragResult.getIntentType() != null ? ragResult.getIntentType().name() : "GENERAL",
                documents,
                ragResult.getRetrievalTimeMs(),
                ragResult.getSearchStrategy()
        );
    }

    @Tool(description = "获取知识库中所有可用的文档列表")
    public List<String> listKnowledgeBaseDocuments() {
        return List.of(
                "attendance-rules.md - 考勤制度",
                "employee-benefits.md - 员工福利",
                "expense-reimbursement.md - 费用报销",
                "hr-leave-rules.md - 请假制度",
                "information-security.md - 信息安全",
                "office-facilities.md - 办公设施",
                "performance-review.md - 绩效考核",
                "resignation-procedure.md - 离职流程"
        );
    }

    @Tool(description = "根据文档名称获取该文档的完整内容摘要")
    public DocumentSummary getDocumentSummary(
            @ToolParam(description = "文档名称，如 attendance-rules.md", required = true) String documentName) {
        
        String summary = switch (documentName) {
            case "attendance-rules.md" -> "考勤制度：包含上下班时间、打卡方式、迟到早退处理、旷工处理、加班申请流程等规定。";
            case "employee-benefits.md" -> "员工福利：包含五险一金、带薪年假、节日福利、年度体检、团建活动等福利内容。";
            case "expense-reimbursement.md" -> "费用报销：包含差旅费报销标准、报销流程、报销时限、票据要求等规定。";
            case "hr-leave-rules.md" -> "请假制度：包含年假、病假、婚假、产假、事假等各类假期的申请流程和规定。";
            case "information-security.md" -> "信息安全：包含公司信息安全政策、数据保密规定、网络使用规范等。";
            case "office-facilities.md" -> "办公设施：包含会议室预约流程、办公设备使用、办公用品申领等规定。";
            case "performance-review.md" -> "绩效考核：包含绩效考核周期、考核指标、评分标准、结果应用等内容。";
            case "resignation-procedure.md" -> "离职流程：包含辞职申请、工作交接、离职手续办理等规定。";
            default -> "未找到该文档的摘要信息，请使用 searchKnowledgeBase 工具检索相关内容。";
        };
        
        return new DocumentSummary(documentName, summary);
    }

    private DocumentInfo toDocumentInfo(SearchResult result) {
        return new DocumentInfo(
                result.getSourceFile(),
                result.getContent(),
                result.getHighlight(),
                result.getScore(),
                result.getSourceType()
        );
    }

    @Data
    public static class KnowledgeSearchResult {
        private String originalQuery;
        private String rewrittenQuery;
        private String intentType;
        private List<DocumentInfo> documents;
        private long retrievalTimeMs;
        private String searchStrategy;

        public KnowledgeSearchResult(String originalQuery, String rewrittenQuery, String intentType,
                                     List<DocumentInfo> documents, long retrievalTimeMs, String searchStrategy) {
            this.originalQuery = originalQuery;
            this.rewrittenQuery = rewrittenQuery;
            this.intentType = intentType;
            this.documents = documents;
            this.retrievalTimeMs = retrievalTimeMs;
            this.searchStrategy = searchStrategy;
        }
    }

    @Data
    public static class DocumentInfo {
        private String sourceFile;
        private String content;
        private String highlight;
        private double score;
        private String sourceType;

        public DocumentInfo(String sourceFile, String content, String highlight, double score, String sourceType) {
            this.sourceFile = sourceFile;
            this.content = content;
            this.highlight = highlight;
            this.score = score;
            this.sourceType = sourceType;
        }
    }

    @Data
    public static class DocumentSummary {
        private String documentName;
        private String summary;

        public DocumentSummary(String documentName, String summary) {
            this.documentName = documentName;
            this.summary = summary;
        }
    }
}
