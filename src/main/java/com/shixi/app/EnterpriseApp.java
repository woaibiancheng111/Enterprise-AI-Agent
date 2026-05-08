package com.shixi.app;

import com.shixi.advisor.BlockedWordAdvisor;
import com.shixi.advisor.MyLogAdvisor;
import com.shixi.agent.ToolOrchestrationService;
import com.shixi.memory.FileBasedChatMemory;
import com.shixi.mcp.EmployeeServiceTools;
import com.shixi.mcp.KnowledgeBaseTools;
import com.shixi.mcp.TimeTools;
import com.shixi.rag.model.SearchResult;
import com.shixi.rag.service.EnhancedRagService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Component
@Slf4j
public class EnterpriseApp {

    private final ChatClient chatClient;
    private final ChatClient chatClientWithTools;
    private final ToolOrchestrationService toolOrchestrationService;
    
    private static final List<String> BLOCKED_WORDS = List.of(
            "薪资明细",
            "商业机密",
            "财报数据",
            "高管隐私"
    );

    private static final String SYSTEM_PROMPT = """
            你是"企业HR与行政AI助手"，专属服务于本公司内部员工。
            你的任务是快速、准确解答员工关于请假、报销、办公设施、福利等行政人事问题。
            
            对话要求：
            1. 第一轮用简短、专业的语气自我介绍。
            2. 回答需基于企业现有规章制度，不瞎编乱造。
            3. 如果用户问题缺少必要信息（如未说明请哪种假、未说明报销类别），请进行1到2个追问（例如：所在部门、申请时长等）。
            4. 如果问题超出HR/行政范畴（如IT网络故障），请礼貌引导至IT部门。
            
            输出风格：
            1. 语气专业、热情、清晰，采用职场沟通风格。
            2. 步骤类问题使用分点列表（如 1. 2. 3.）。
            3. 默认控制篇幅，不超过200字；若遇复杂流程则重点突出第一步。
            
            安全边界：
            1. 绝对不讨论其他员工薪水、公司机密及未公开财务状况。
            2. 遇到此类打探直接回复："抱歉，这属于公司保密信息，无法为您解答。"
            """;

    private static final String SYSTEM_PROMPT_WITH_TOOLS = """
            你是"企业HR与行政AI助手"，专属服务于本公司内部员工。
            你的任务是快速、准确解答员工关于请假、报销、办公设施、福利等行政人事问题。
            
            你可以使用以下工具来帮助回答问题，调用工具时必须使用精确的工具名称：
            
            【时间工具】
            - getCurrentDateTime：获取当前日期和时间
            - getCurrentDate：获取当前日期
            - getDayOfWeek：获取指定日期是星期几（参数：date，格式yyyy-MM-dd）
            - calculateDaysBetween：计算两个日期之间的天数（参数：startDate, endDate）
            - calculateWorkDaysBetween：计算两个日期之间的工作日天数（参数：startDate, endDate）
            - isWorkDay：检查指定日期是否为工作日（参数：date）
            - isWeekend：检查指定日期是否为周末（参数：date）
            - addDays：获取指定日期之后N天的日期（参数：startDate, days）
            - minusDays：获取指定日期之前N天的日期（参数：startDate, days）
            - getWorkDaysOfMonth：获取指定月份的所有工作日列表（参数：year, month）
            - formatDate：格式化日期（参数：date, inputFormat, outputFormat）
            
            【员工服务工具】
            - getEmployeeInfo：根据员工ID查询员工基本信息（参数：employeeId）
            - findEmployeeIdsByName：根据员工姓名查询员工ID列表（参数：name）
            - getEmployeesByDepartment：查询指定部门的所有员工ID列表（参数：department）
            - getLeaveBalance：查询员工的假期余额（参数：employeeId）
            - applyLeave：申请请假（参数：employeeId, leaveType, startDate, endDate, reason）
            - getLeaveApplicationStatus：查询请假申请状态（参数：applicationId）
            - getEmployeeLeaveApplications：查询员工的所有请假申请（参数：employeeId）
            - applyReimbursement：申请报销（参数：employeeId, type, amount, description, invoiceNumber）
            - getReimbursementStatus：查询报销申请状态（参数：applicationId）
            - getAllDepartments：获取所有部门列表
            
            【知识库检索工具】
            - searchKnowledgeBase：检索企业知识库中的相关文档（参数：query, topK可选）
            - listKnowledgeBaseDocuments：获取知识库中所有可用的文档列表
            - getDocumentSummary：根据文档名称获取该文档的完整内容摘要（参数：documentName）
            
            当用户询问以下问题时，请使用相应的工具：
            - 涉及日期计算、工作日计算：使用对应的时间工具
            - 涉及请假申请、报销申请、员工信息查询：使用对应的员工服务工具
            - 涉及公司规章制度、政策查询：使用 searchKnowledgeBase 或 getDocumentSummary
            
            重要提示：
            - 调用工具时，必须使用上述精确的工具名称，不要使用其他名称
            - 如果缺少必要参数，请先向用户询问
            - 工具名称区分大小写
            
            对话要求：
            1. 第一轮用简短、专业的语气自我介绍。
            2. 回答需基于企业现有规章制度，不瞎编乱造。
            3. 如果用户问题缺少必要信息（如未说明请哪种假、未说明报销类别），请进行1到2个追问（例如：所在部门、申请时长等）。
            4. 如果问题超出HR/行政范畴（如IT网络故障），请礼貌引导至IT部门。
            
            输出风格：
            1. 语气专业、热情、清晰，采用职场沟通风格。
            2. 步骤类问题使用分点列表（如 1. 2. 3.）。
            3. 默认控制篇幅，不超过200字；若遇复杂流程则重点突出第一步。
            
            安全边界：
            1. 绝对不讨论其他员工薪水、公司机密及未公开财务状况。
            2. 遇到此类打探直接回复："抱歉，这属于公司保密信息，无法为您解答。"
            """;

    public EnterpriseApp(
            ChatModel dashscopeChatModel,
            TimeTools timeTools,
            EmployeeServiceTools employeeServiceTools,
            KnowledgeBaseTools knowledgeBaseTools,
            ToolOrchestrationService toolOrchestrationService) {

        this.toolOrchestrationService = toolOrchestrationService;

        ChatMemory chatMemory = new FileBasedChatMemory(Path.of("data","chat-memory"));

        this.chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        new BlockedWordAdvisor(BLOCKED_WORDS),
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        new MyLogAdvisor()
                )
                .build();
        
        this.chatClientWithTools = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT_WITH_TOOLS)
                .defaultAdvisors(
                        new BlockedWordAdvisor(BLOCKED_WORDS),
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        new MyLogAdvisor()
                )
                .defaultTools(MethodToolCallbackProvider.builder()
                        .toolObjects(timeTools, employeeServiceTools, knowledgeBaseTools)
                        .build())
                .build();
    }

    public String doChat(String message, String chatId) {
        Optional<String> businessOperation = tryHandleBusinessOperation(message);
        if (businessOperation.isPresent()) {
            return businessOperation.get();
        }

        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .chatResponse();

        String content = null;
        if (response != null) {
            content = response.getResult().getOutput().getText();
        }
        log.info("content: {}", content);
        return content;
    }

    public record EmployeeTicket(
            String ticketId,
            String employeeName,
            String employeeId,
            String department,
            String requirementType,
            String title,
            String description,
            String priority,
            String status,
            String assigneeGroup,
            String sla,
            List<String> requiredFields,
            List<String> missingFields,
            List<String> actionItems,
            String createdAt) {
    }

    record RawEmployeeTicket(String requirementType, String title, String description,
                             String priority, List<String> requiredFields,
                             List<String> missingFields, List<String> actionItems) {
    }

    public EmployeeTicket doChatWithReport(String message, String chatId) {
        RawEmployeeTicket rawTicket;
        try {
            rawTicket = chatClient
                    .prompt()
                    .system("""
                            你是企业服务台工单整理助手。
                            请把用户诉求整理成结构化工单草稿，只输出字段值，不要执行真实业务。
                            requirementType 应简短，例如：请假处理、报销异常、办公用品申领、账号权限、行政咨询。
                            priority 只能是 P0、P1、P2、P3；普通办公用品和常规咨询为 P3，请假/报销为 P2，紧急投诉为 P1/P0。
                            requiredFields 是正式提交该类工单需要收集的字段；missingFields 是当前用户描述仍缺少的字段。
                            actionItems 是服务台接下来应该做的动作，3 到 5 条。
                            """)
                    .user(message)
                    .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                            .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                    .call()
                    .entity(RawEmployeeTicket.class);
        } catch (Exception e) {
            rawTicket = fallbackTicket(message);
        }

        EmployeeTicket ticket = enrichTicket(rawTicket, message);

        log.info("Ticket generated: {}", ticket);
        return ticket;
    }

    private EmployeeTicket enrichTicket(RawEmployeeTicket rawTicket, String message) {
        com.shixi.security.CurrentUser user = com.shixi.security.CurrentUserContext.require();
        String requirementType = defaultText(rawTicket.requirementType(), detectRequirementType(message));
        String priority = normalizePriority(rawTicket.priority(), requirementType, message);
        return new EmployeeTicket(
                generateTicketId(requirementType),
                user.displayName(),
                user.employeeId(),
                "待服务台确认",
                requirementType,
                defaultText(rawTicket.title(), requirementType + "工单"),
                defaultText(rawTicket.description(), message),
                priority,
                rawTicket.missingFields() == null || rawTicket.missingFields().isEmpty() ? "READY_TO_SUBMIT" : "NEED_MORE_INFO",
                detectAssigneeGroup(requirementType),
                detectSla(priority),
                defaultList(rawTicket.requiredFields(), defaultRequiredFields(requirementType)),
                defaultList(rawTicket.missingFields(), List.of()),
                defaultList(rawTicket.actionItems(), defaultActionItems(requirementType)),
                LocalDateTime.now().toString()
        );
    }

    private RawEmployeeTicket fallbackTicket(String message) {
        String requirementType = detectRequirementType(message);
        return new RawEmployeeTicket(
                requirementType,
                requirementType + "工单",
                message,
                "P3",
                defaultRequiredFields(requirementType),
                defaultRequiredFields(requirementType),
                defaultActionItems(requirementType)
        );
    }

    private String generateTicketId(String requirementType) {
        String prefix = switch (requirementType) {
            case "请假处理" -> "TK-LEAVE";
            case "报销异常" -> "TK-REIM";
            case "办公用品申领" -> "TK-OFFICE";
            case "账号权限" -> "TK-ACCESS";
            default -> "TK-SVC";
        };
        return prefix + "-" + LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + java.util.UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private String detectRequirementType(String message) {
        if (containsAny(message, "请假", "年假", "病假", "事假", "休假")) return "请假处理";
        if (containsAny(message, "报销", "发票", "差旅", "打车", "交通费")) return "报销异常";
        if (containsAny(message, "办公用品", "电脑", "鼠标", "键盘", "工牌", "设备")) return "办公用品申领";
        if (containsAny(message, "账号", "权限", "登录", "密码")) return "账号权限";
        return "行政咨询";
    }

    private String detectAssigneeGroup(String requirementType) {
        return switch (requirementType) {
            case "请假处理" -> "HR 服务台";
            case "报销异常" -> "财务服务台";
            case "办公用品申领" -> "行政服务台";
            case "账号权限" -> "IT 服务台";
            default -> "综合服务台";
        };
    }

    private String normalizePriority(String priority, String requirementType, String message) {
        if (List.of("P0", "P1", "P2", "P3").contains(priority)) {
            return priority;
        }
        if (containsAny(message, "紧急", "马上", "今天必须", "严重")) {
            return "P1";
        }
        if ("请假处理".equals(requirementType) || "报销异常".equals(requirementType)) {
            return "P2";
        }
        return "P3";
    }

    private String detectSla(String priority) {
        return switch (priority) {
            case "P0" -> "2 小时内响应";
            case "P1" -> "4 小时内响应";
            case "P2" -> "1 个工作日内响应";
            default -> "2 个工作日内响应";
        };
    }

    private List<String> defaultRequiredFields(String requirementType) {
        return switch (requirementType) {
            case "请假处理" -> List.of("假期类型", "开始日期", "结束日期或天数", "请假原因");
            case "报销异常" -> List.of("报销类型", "金额", "费用日期", "发票或凭证", "说明");
            case "办公用品申领" -> List.of("物品名称", "数量", "用途", "期望领取日期");
            case "账号权限" -> List.of("系统名称", "权限范围", "业务原因", "期望开通时间");
            default -> List.of("诉求说明", "期望处理时间");
        };
    }

    private List<String> defaultActionItems(String requirementType) {
        return switch (requirementType) {
            case "办公用品申领" -> List.of("确认物品名称、数量和用途", "检查行政库存", "提交行政服务台审批", "通知员工领取时间");
            case "请假处理" -> List.of("确认假期类型、日期和天数", "校验假期余额", "生成请假申请", "通知直属负责人审批");
            case "报销异常" -> List.of("确认报销类型和金额", "核对发票或凭证", "提交财务服务台", "同步处理进度");
            default -> List.of("补齐必要字段", "分派到对应服务台", "跟踪处理状态");
        };
    }

    private boolean containsAny(String text, String... keywords) {
        if (text == null) {
            return false;
        }
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String defaultText(String text, String defaultValue) {
        return text == null || text.isBlank() ? defaultValue : text;
    }

    private List<String> defaultList(List<String> list, List<String> defaultValue) {
        return list == null || list.isEmpty() ? defaultValue : list;
    }


    // 知识库问答功能
    @Resource
    private VectorStore enterpriseAppVectorStore;
    @Resource
    private com.shixi.rag.service.EnhancedRagService enhancedRagService;

    /**
     * 用Rag知识库进行对话
     * @param message
     * @param chatId
     */
    public String doChatWithKnowledgeBase(String message, String chatId) {
        Optional<String> businessOperation = tryHandleBusinessOperation(message);
        if (businessOperation.isPresent()) {
            return businessOperation.get();
        }

        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .advisors(new MyLogAdvisor())
                .advisors(new QuestionAnswerAdvisor(enterpriseAppVectorStore))
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    /**
     * 流式知识库问答（带引用溯源）
     * @param message 用户消息
     * @param chatId 会话ID
     * @param topK 返回结果数量
     * @return 内容块的Flux流
     */
    public Flux<String> streamChatWithKnowledgeBaseWithCitations(String message, String chatId, int topK) {
        Optional<String> businessOperation = tryHandleBusinessOperation(message);
        if (businessOperation.isPresent()) {
            return Flux.just(businessOperation.get());
        }

        // 1. 执行 RAG 检索获取引用
        EnhancedRagService.RagResult ragResult = enhancedRagService.search(message, topK);

        // 2. 构建引用文本
        String citationsText = buildCitationsText(ragResult);

        // 3. 流式调用 AI
        Flux<String> aiStream = chatClient
                .prompt()
                .user(buildUserMessageWithKnowledgeBase(message, ragResult))
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .stream()
                .content();

        // 4. 先发送 AI 回复，最后附加引用，便于前端稳定拆分正文和来源
        if (!citationsText.isEmpty()) {
            return aiStream.concatWith(Flux.just("\n\n---\n" + citationsText));
        }
        return aiStream;
    }

    /**
     * 构建知识库问答的用户消息
     */
    private String buildUserMessageWithKnowledgeBase(String message, EnhancedRagService.RagResult ragResult) {
        StringBuilder context = new StringBuilder();
        context.append("【相关文档】\n");
        for (int i = 0; i < ragResult.getResults().size(); i++) {
            com.shixi.rag.model.SearchResult result = ragResult.getResults().get(i);
            context.append(String.format("【文档 %d】来源: %s\n%s\n\n",
                    i + 1,
                    result.getSourceFile(),
                    result.getContent()));
        }
        context.append("【用户问题】\n").append(message);
        return context.toString();
    }

    /**
     * 构建引用文本（按文档去重）
     */
    private String buildCitationsText(EnhancedRagService.RagResult ragResult) {
        if (ragResult.getResults().isEmpty()) {
            return "";
        }
        // 按文档名分组
        Map<String, List<SearchResult>> groupedByFile = ragResult.getResults().stream()
                .collect(java.util.stream.Collectors.groupingBy(SearchResult::getSourceFile));

        StringBuilder sb = new StringBuilder();
        sb.append("【参考来源】\n");
        int docIndex = 1;
        for (Map.Entry<String, List<SearchResult>> entry : groupedByFile.entrySet()) {
            sb.append(String.format("[%d] %s (%d处相关)\n", docIndex++, entry.getKey(), entry.getValue().size()));
        }
        return sb.toString();
    }

    // ==================== 流式输出方法 ====================

    /**
     * 流式对话
     * @param message 用户消息
     * @param chatId 会话ID
     * @return 内容块的Flux流
     */
    public Flux<String> streamChat(String message, String chatId) {
        Optional<String> businessOperation = tryHandleBusinessOperation(message);
        if (businessOperation.isPresent()) {
            return Flux.just(businessOperation.get());
        }

        return chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .stream()
                .content();
    }

    /**
     * 流式知识库问答
     * @param message 用户消息
     * @param chatId 会话ID
     * @return 内容块的Flux流
     */
    public Flux<String> streamChatWithKnowledgeBase(String message, String chatId) {
        Optional<String> businessOperation = tryHandleBusinessOperation(message);
        if (businessOperation.isPresent()) {
            return Flux.just(businessOperation.get());
        }

        return chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .advisors(new QuestionAnswerAdvisor(enterpriseAppVectorStore))
                .stream()
                .content();
    }

    // ==================== MCP 工具对话方法 ====================

    /**
     * 使用MCP工具进行对话（AI可以自动调用工具）
     * @param message 用户消息
     * @param chatId 会话ID
     * @return AI回复
     */
    public String doChatWithTools(String message, String chatId) {
        return toolOrchestrationService.tryHandle(message)
                .orElseGet(() -> doChatWithLlmTools(message, chatId));
    }

    private String doChatWithLlmTools(String message, String chatId) {
        try {
            ChatResponse response = chatClientWithTools
                    .prompt()
                    .user(message)
                    .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                            .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                    .call()
                    .chatResponse();

            String content = null;
            if (response != null) {
                content = response.getResult().getOutput().getText();
            }
            log.info("content (with tools): {}", content);
            return content;
        } catch (Exception e) {
            log.warn("LLM 工具调用失败，已返回兜底提示: {}", e.getMessage());
            return """
                    智能工具调用暂时无法完成该请求。

                    你可以尝试补充更明确的工具参数，例如：
                    - 查询员工：`帮我查询员工 E001 的基本信息`
                    - 查询假期：`查询员工 E001 的假期余额`
                    - 申请请假：`帮员工 E001 申请 3 天年假，开始日期 2026-05-01`
                    - 日期计算：`今天是星期几，到月底还有多少个工作日`
                    """;
        }
    }

    /**
     * 使用MCP工具进行流式对话
     * @param message 用户消息
     * @param chatId 会话ID
     * @return 内容块的Flux流
     */
    public Flux<String> streamChatWithTools(String message, String chatId) {
        return Flux.just(doChatWithTools(message, chatId));
    }

    private Optional<String> tryHandleBusinessOperation(String message) {
        return toolOrchestrationService.tryHandleBusinessOperation(message);
    }
}
