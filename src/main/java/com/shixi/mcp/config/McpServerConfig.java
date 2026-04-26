package com.shixi.mcp.config;

import com.shixi.mcp.EmployeeServiceTools;
import com.shixi.mcp.KnowledgeBaseTools;
import com.shixi.mcp.TimeTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpServerConfig {

    @Bean
    public ToolCallbackProvider enterpriseMcpTools(
            TimeTools timeTools,
            EmployeeServiceTools employeeServiceTools,
            KnowledgeBaseTools knowledgeBaseTools) {
        
        return MethodToolCallbackProvider.builder()
                .toolObjects(timeTools, employeeServiceTools, knowledgeBaseTools)
                .build();
    }
}
