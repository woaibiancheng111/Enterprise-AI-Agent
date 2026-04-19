package com.shixi.app;

import com.shixi.advisor.BlockedWordAdvisor;
import com.shixi.advisor.MyLogAdvisor;
import com.shixi.memory.FileBasedChatMemory;
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
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Component
@Slf4j
public class EnterpriseApp {

    private final ChatClient chatClient;
    private static final List<String> BLOCKED_WORDS = List.of(
            "薪资明细",
            "商业机密",
            "财报数据",
            "高管隐私"
    );

    private static final String SYSTEM_PROMPT = """
            你是“企业HR与行政AI助手”，专属服务于本公司内部员工。
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
            2. 遇到此类打探直接回复：“抱歉，这属于公司保密信息，无法为您解答。”
            """;

    public EnterpriseApp(ChatModel dashscopeChatModel) {

        ChatMemory chatMemory = new FileBasedChatMemory(Path.of("data","chat-memory"));


        this.chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        new BlockedWordAdvisor(BLOCKED_WORDS),
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        new MyLogAdvisor()
                )
                .build();
    }

    public String doChat(String message, String chatId) {
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

    record EmployeeTicket(String employeeName, String department, String requirementType, List<String> actionItems) {

    }

    public EmployeeTicket doChatWithReport(String message, String chatId) {
        EmployeeTicket ticket = chatClient
                .prompt()
                .system(SYSTEM_PROMPT)
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .entity(EmployeeTicket.class);

        log.info("Ticket generated: {}", ticket);
        return ticket;
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

        // 4. 先发送引用，再发送 AI 回复
        if (!citationsText.isEmpty()) {
            return Flux.just(citationsText + "\n\n")
                    .concatWith(aiStream);
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
        return chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .advisors(new QuestionAnswerAdvisor(enterpriseAppVectorStore))
                .stream()
                .content();
    }
}
