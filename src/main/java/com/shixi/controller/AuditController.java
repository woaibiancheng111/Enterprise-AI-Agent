package com.shixi.controller;

import com.shixi.business.entity.ToolCallLogEntity;
import com.shixi.business.service.ToolAuditService;
import com.shixi.security.CurrentUser;
import com.shixi.security.CurrentUserContext;
import com.shixi.security.ForbiddenException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("audit")
public class AuditController {

    private final ToolAuditService toolAuditService;

    public AuditController(ToolAuditService toolAuditService) {
        this.toolAuditService = toolAuditService;
    }

    @GetMapping("/tool-calls")
    public Object toolCalls(
            @RequestParam(required = false) String toolName,
            @RequestParam(required = false) String employeeId,
            @RequestParam(defaultValue = "50") int limit) {
        CurrentUser user = CurrentUserContext.require();
        if (!user.isHr() && !user.isAdmin()) {
            throw new ForbiddenException("当前账号无权查看工具审计日志");
        }
        List<ToolCallLogEntity> logs = toolAuditService.listRecent(toolName, employeeId, limit);
        return Map.of(
                "success", true,
                "count", logs.size(),
                "logs", logs
        );
    }
}
