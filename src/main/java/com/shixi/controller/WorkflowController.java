package com.shixi.controller;

import com.shixi.business.service.EmployeeBusinessService;
import com.shixi.business.service.ToolAuditService;
import com.shixi.security.CurrentUser;
import com.shixi.security.CurrentUserContext;
import com.shixi.security.ForbiddenException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("workflow")
public class WorkflowController {

    private final EmployeeBusinessService employeeBusinessService;
    private final ToolAuditService toolAuditService;

    public WorkflowController(EmployeeBusinessService employeeBusinessService, ToolAuditService toolAuditService) {
        this.employeeBusinessService = employeeBusinessService;
        this.toolAuditService = toolAuditService;
    }

    @GetMapping("/applications")
    public Object applications(
            @RequestParam(defaultValue = "all") String type,
            @RequestParam(defaultValue = "all") String status) {
        assertHrOrAdmin();
        List<EmployeeBusinessService.WorkflowApplication> applications =
                employeeBusinessService.listWorkflowApplications(type, status);
        return Map.of(
                "success", true,
                "count", applications.size(),
                "applications", applications
        );
    }

    @PostMapping("/leave/{applicationId}/review")
    public EmployeeBusinessService.ReviewResult reviewLeave(
            @PathVariable String applicationId,
            @RequestBody ReviewRequest request) {
        CurrentUser reviewer = assertHrOrAdmin();
        EmployeeBusinessService.ReviewResult result = employeeBusinessService.reviewLeaveApplication(
                applicationId,
                request.decision(),
                request.comment(),
                reviewer.userId()
        );
        audit("reviewLeaveApplication", result, request);
        return result;
    }

    @PostMapping("/reimbursement/{applicationId}/review")
    public EmployeeBusinessService.ReviewResult reviewReimbursement(
            @PathVariable String applicationId,
            @RequestBody ReviewRequest request) {
        CurrentUser reviewer = assertHrOrAdmin();
        EmployeeBusinessService.ReviewResult result = employeeBusinessService.reviewReimbursementApplication(
                applicationId,
                request.decision(),
                request.comment(),
                reviewer.userId()
        );
        audit("reviewReimbursementApplication", result, request);
        return result;
    }

    private CurrentUser assertHrOrAdmin() {
        CurrentUser user = CurrentUserContext.require();
        if (!user.isHr() && !user.isAdmin()) {
            throw new ForbiddenException("当前账号无权访问审批管理");
        }
        return user;
    }

    private void audit(String toolName, EmployeeBusinessService.ReviewResult result, ReviewRequest request) {
        EmployeeBusinessService.WorkflowApplication application = result.application();
        toolAuditService.record(
                toolName,
                "workflow",
                application.employeeId(),
                Map.of(
                        "applicationId", application.applicationId(),
                        "decision", request.decision(),
                        "comment", request.comment()
                ),
                result,
                result.success()
        );
    }

    public record ReviewRequest(String decision, String comment) {
    }
}
