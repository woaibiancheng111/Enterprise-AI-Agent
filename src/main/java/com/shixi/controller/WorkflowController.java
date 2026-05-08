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

import java.time.LocalDate;
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

    @PostMapping("/leave")
    public SubmitLeaveResponse submitLeave(@RequestBody SubmitLeaveRequest request) {
        CurrentUser user = CurrentUserContext.require();
        String employeeId = request.employeeId() == null || request.employeeId().isBlank()
                ? user.employeeId()
                : request.employeeId();
        if (employeeId == null || employeeId.isBlank()) {
            throw new IllegalArgumentException("员工工号不能为空");
        }
        if (!user.canAccessEmployee(employeeId)) {
            throw new ForbiddenException("当前账号无权为该员工提交请假申请");
        }
        if (request.autoApprove() && !user.isHr() && !user.isAdmin()) {
            throw new ForbiddenException("只有 HR/Admin 可以启用自动审批");
        }

        EmployeeBusinessService.LeaveApplicationResult submitResult = employeeBusinessService.applyLeave(
                employeeId,
                request.leaveType(),
                LocalDate.parse(request.startDate()),
                LocalDate.parse(request.endDate()),
                request.reason()
        );
        audit("submitLeaveApplication", submitResult, request, employeeId);

        if (!submitResult.success()) {
            return new SubmitLeaveResponse(false, submitResult.message(), null);
        }

        EmployeeBusinessService.WorkflowApplication application = findWorkflowApplication(
                employeeId,
                submitResult.applicationId()
        );
        String message = submitResult.message();
        if (request.autoApprove()) {
            EmployeeBusinessService.ReviewResult reviewResult = employeeBusinessService.reviewLeaveApplication(
                    submitResult.applicationId(),
                    "APPROVED",
                    "自动审批通过",
                    user.userId()
            );
            audit("autoApproveLeaveApplication", reviewResult, request, employeeId);
            application = reviewResult.application();
            message = submitResult.message() + "，已自动审批通过";
        }
        return new SubmitLeaveResponse(true, message, application);
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

    private void audit(String toolName, EmployeeBusinessService.LeaveApplicationResult result,
                       SubmitLeaveRequest request, String employeeId) {
        toolAuditService.record(
                toolName,
                "workflow",
                employeeId,
                request,
                result,
                result.success()
        );
    }

    private void audit(String toolName, EmployeeBusinessService.ReviewResult result,
                       SubmitLeaveRequest request, String employeeId) {
        toolAuditService.record(
                toolName,
                "workflow",
                employeeId,
                request,
                result,
                result.success()
        );
    }

    private EmployeeBusinessService.WorkflowApplication findWorkflowApplication(String employeeId, String applicationId) {
        return employeeBusinessService.listWorkflowApplicationsByEmployee(employeeId).stream()
                .filter(item -> item.applicationId().equalsIgnoreCase(applicationId))
                .findFirst()
                .orElse(null);
    }

    public record ReviewRequest(String decision, String comment) {
    }

    public record SubmitLeaveRequest(
            String employeeId,
            String leaveType,
            String startDate,
            String endDate,
            String reason,
            boolean autoApprove) {
    }

    public record SubmitLeaveResponse(
            boolean success,
            String message,
            EmployeeBusinessService.WorkflowApplication application) {
    }
}
