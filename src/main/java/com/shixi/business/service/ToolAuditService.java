package com.shixi.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shixi.business.entity.ToolCallLogEntity;
import com.shixi.business.mapper.ToolCallLogMapper;
import com.shixi.security.CurrentUserContext;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ToolAuditService {

    private static final int MAX_TEXT_LENGTH = 2000;

    private final ToolCallLogMapper toolCallLogMapper;
    private final ObjectMapper objectMapper;

    public ToolAuditService(ToolCallLogMapper toolCallLogMapper, ObjectMapper objectMapper) {
        this.toolCallLogMapper = toolCallLogMapper;
        this.objectMapper = objectMapper;
    }

    public void record(String toolName, String domain, String targetEmployeeId,
                       Object input, Object result, boolean success) {
        ToolCallLogEntity log = new ToolCallLogEntity(
                null,
                toolName,
                domain,
                CurrentUserContext.get().map(user -> user.userId()).orElse("system"),
                blankToNull(targetEmployeeId),
                truncate(toJson(input)),
                truncate(toJson(result)),
                success,
                LocalDateTime.now()
        );
        toolCallLogMapper.insert(log);
    }

    public List<ToolCallLogEntity> listRecent(String toolName, String employeeId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        LambdaQueryWrapper<ToolCallLogEntity> query = new LambdaQueryWrapper<ToolCallLogEntity>()
                .orderByDesc(ToolCallLogEntity::getCreatedAt)
                .orderByDesc(ToolCallLogEntity::getId)
                .last("LIMIT " + safeLimit);
        if (toolName != null && !toolName.isBlank()) {
            query.eq(ToolCallLogEntity::getToolName, toolName.trim());
        }
        if (employeeId != null && !employeeId.isBlank()) {
            query.eq(ToolCallLogEntity::getTargetEmployeeId, employeeId.trim().toUpperCase());
        }
        return toolCallLogMapper.selectList(query);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String string) {
            return string;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return String.valueOf(value);
        }
    }

    private String truncate(String value) {
        if (value == null || value.length() <= MAX_TEXT_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_TEXT_LENGTH) + "...";
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
