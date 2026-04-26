package com.shixi.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * MCP 集成门面。
 *
 * Spring AI 的 MCP Server 负责对外暴露协议端点；这个服务给业务前端提供
 * 一个轻量 Bridge，用于查看 MCP 工具清单并执行本地 MCP 工具。
 */
@Service
@Slf4j
public class McpIntegrationService {

    private final ToolCallbackProvider toolCallbackProvider;
    private final ToolOrchestrationService toolOrchestrationService;
    private final ObjectMapper objectMapper;

    public McpIntegrationService(
            @Qualifier("enterpriseMcpTools") ToolCallbackProvider toolCallbackProvider,
            ToolOrchestrationService toolOrchestrationService,
            ObjectMapper objectMapper) {
        this.toolCallbackProvider = toolCallbackProvider;
        this.toolOrchestrationService = toolOrchestrationService;
        this.objectMapper = objectMapper;
    }

    public McpStatus status() {
        List<McpToolCard> tools = tools();
        return new McpStatus(
                true,
                "enterprise-mcp-server",
                "SYNC",
                "/api/sse",
                "/api/mcp/message",
                tools.size(),
                tools.stream().map(McpToolCard::domain).distinct().sorted().toList(),
                LocalDateTime.now().toString()
        );
    }

    public List<McpToolCard> tools() {
        return Arrays.stream(toolCallbackProvider.getToolCallbacks())
                .map(this::toToolCard)
                .sorted(Comparator.comparing(McpToolCard::domain).thenComparing(McpToolCard::name))
                .toList();
    }

    public McpChatResponse chat(String message) {
        if (message != null && containsAny(message.toLowerCase(Locale.ROOT), "列出", "工具", "tool", "tools", "清单")) {
            StringBuilder content = new StringBuilder();
            content.append("当前 MCP Server 已注册 ").append(tools().size()).append(" 个工具：\n");
            for (McpToolCard tool : tools()) {
                content.append("- `")
                        .append(tool.name())
                        .append("` [")
                        .append(tool.domain())
                        .append("]：")
                        .append(tool.description())
                        .append("\n");
            }
            return new McpChatResponse(
                    true,
                    "enterprise-mcp-server",
                    "listTools",
                    content.toString(),
                    "已通过 MCP Bridge 读取 ToolCallbackProvider 工具注册表。",
                    LocalDateTime.now().toString()
            );
        }

        Optional<String> handled = toolOrchestrationService.tryHandle(message);
        if (handled.isPresent()) {
            return new McpChatResponse(
                    true,
                    "mcp-local-bridge",
                    inferToolName(message),
                    handled.get(),
                    "已通过 MCP Bridge 调用本地工具能力。",
                    LocalDateTime.now().toString()
            );
        }

        return new McpChatResponse(
                false,
                "mcp-local-bridge",
                null,
                "未识别到可执行的 MCP 工具意图。请尝试：查询员工 E001、申请 3 天年假、查询假期余额、计算到月底工作日。",
                "没有匹配到明确工具或必要参数不足。",
                LocalDateTime.now().toString()
        );
    }

    public McpCallResponse callTool(String toolName, Map<String, Object> arguments) {
        FunctionCallback toolCallback = findTool(toolName)
                .orElseThrow(() -> new IllegalArgumentException("MCP 工具不存在: " + toolName));

        try {
            String input = objectMapper.writeValueAsString(arguments == null ? Map.of() : arguments);
            String output = toolCallback.call(input);
            return new McpCallResponse(
                    true,
                    toolCallback.getName(),
                    domainOf(toolCallback.getName()),
                    input,
                    output,
                    LocalDateTime.now().toString()
            );
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("工具参数无法序列化为 JSON", e);
        } catch (Exception e) {
            log.warn("MCP 工具执行失败: {}", toolName, e);
            return new McpCallResponse(
                    false,
                    toolCallback.getName(),
                    domainOf(toolCallback.getName()),
                    String.valueOf(arguments),
                    "工具执行失败：" + e.getMessage(),
                    LocalDateTime.now().toString()
            );
        }
    }

    private Optional<FunctionCallback> findTool(String toolName) {
        if (toolName == null || toolName.isBlank()) {
            return Optional.empty();
        }
        return Arrays.stream(toolCallbackProvider.getToolCallbacks())
                .filter(tool -> tool.getName().equals(toolName))
                .findFirst();
    }

    private McpToolCard toToolCard(FunctionCallback toolCallback) {
        return new McpToolCard(
                toolCallback.getName(),
                domainOf(toolCallback.getName()),
                toolCallback.getDescription(),
                toolCallback.getInputTypeSchema()
        );
    }

    private String domainOf(String toolName) {
        String lowerName = toolName.toLowerCase(Locale.ROOT);
        if (lowerName.contains("employee")
                || lowerName.contains("leave")
                || lowerName.contains("reimbursement")
                || lowerName.contains("department")) {
            return "employee-service";
        }
        if (lowerName.contains("date")
                || lowerName.contains("day")
                || lowerName.contains("week")
                || lowerName.contains("month")) {
            return "time";
        }
        if (lowerName.contains("knowledge") || lowerName.contains("document")) {
            return "knowledge-base";
        }
        return "general";
    }

    private String inferToolName(String message) {
        String lowerMessage = message.toLowerCase(Locale.ROOT);
        if (lowerMessage.contains("基本信息") || lowerMessage.contains("员工信息")) return "getEmployeeInfo";
        if (lowerMessage.contains("假期余额") || lowerMessage.contains("还剩")) return "getLeaveBalance";
        if (lowerMessage.contains("请假") || lowerMessage.contains("年假")) return "applyLeave";
        if (lowerMessage.contains("报销")) return "applyReimbursement";
        if (lowerMessage.contains("工作日") || lowerMessage.contains("星期") || lowerMessage.contains("今天")) {
            return "time-tools";
        }
        if (lowerMessage.contains("制度") || lowerMessage.contains("知识库") || lowerMessage.contains("政策")) {
            return "searchKnowledgeBase";
        }
        return null;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    public record McpStatus(
            boolean enabled,
            String serverName,
            String serverType,
            String sseEndpoint,
            String messageEndpoint,
            int toolCount,
            List<String> domains,
            String checkedAt
    ) {
    }

    public record McpToolCard(String name, String domain, String description, String inputSchema) {
    }

    public record McpChatResponse(
            boolean success,
            String serverName,
            String selectedTool,
            String content,
            String trace,
            String createdAt
    ) {
    }

    public record McpCallResponse(
            boolean success,
            String toolName,
            String domain,
            String inputJson,
            String output,
            String createdAt
    ) {
    }
}
