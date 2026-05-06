package com.shixi.controller;

import com.shixi.agent.McpIntegrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("mcp")
public class McpController {

    private final McpIntegrationService mcpIntegrationService;

    public McpController(McpIntegrationService mcpIntegrationService) {
        this.mcpIntegrationService = mcpIntegrationService;
    }

    @GetMapping("/status")
    public McpIntegrationService.McpStatus status() {
        return mcpIntegrationService.status();
    }

    @GetMapping("/tools")
    public Object tools() {
        return Map.of(
                "success", true,
                "tools", mcpIntegrationService.tools()
        );
    }

    @GetMapping("/chat")
    public McpIntegrationService.McpChatResponse chat(@RequestParam String message) {
        return mcpIntegrationService.chat(message);
    }

    @PostMapping("/chat")
    public McpIntegrationService.McpChatResponse chat(@RequestBody McpChatRequest request) {
        return mcpIntegrationService.chat(request.message());
    }

    @PostMapping("/call")
    public ResponseEntity<McpIntegrationService.McpCallResponse> call(@RequestBody McpToolCallRequest request) {
        return ResponseEntity.ok(mcpIntegrationService.callTool(request.toolName(), request.arguments()));
    }

    public record McpChatRequest(String message) {
    }

    public record McpToolCallRequest(String toolName, Map<String, Object> arguments) {
    }
}
