package com.shixi.mcp;

import com.shixi.business.entity.EmployeeEntity;
import com.shixi.business.entity.LeaveApplicationEntity;
import com.shixi.business.entity.LeaveBalanceEntity;
import com.shixi.business.entity.ReimbursementApplicationEntity;
import com.shixi.business.service.EmployeeBusinessService;
import com.shixi.business.service.ToolAuditService;
import com.shixi.security.CurrentUser;
import com.shixi.security.CurrentUserContext;
import com.shixi.security.ForbiddenException;
import lombok.Data;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class EmployeeServiceTools {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final EmployeeBusinessService employeeBusinessService;
    private final ToolAuditService toolAuditService;

    public EmployeeServiceTools(EmployeeBusinessService employeeBusinessService, ToolAuditService toolAuditService) {
        this.employeeBusinessService = employeeBusinessService;
        this.toolAuditService = toolAuditService;
    }

    @Tool(description = "根据员工ID查询员工基本信息")
    public EmployeeInfo getEmployeeInfo(
            @ToolParam(description = "员工ID，如 E001", required = true) String employeeId) {
        assertCanAccessEmployee(employeeId);
        EmployeeInfo result = employeeBusinessService.findEmployee(employeeId)
                .map(this::toEmployeeInfo)
                .orElse(null);
        audit("getEmployeeInfo", employeeId, input("employeeId", employeeId), result, result != null);
        return result;
    }

    @Tool(description = "根据员工姓名查询员工ID列表（可能有重名）")
    public List<String> findEmployeeIdsByName(
            @ToolParam(description = "员工姓名", required = true) String name) {
        assertHrOrAdmin();
        List<String> result = employeeBusinessService.findEmployeeIdsByName(name);
        audit("findEmployeeIdsByName", null, input("name", name), result, true);
        return result;
    }

    @Tool(description = "查询指定部门的所有员工ID列表")
    public List<String> getEmployeesByDepartment(
            @ToolParam(description = "部门名称，如 技术部、人事部", required = true) String department) {
        assertHrOrAdmin();
        List<String> result = employeeBusinessService.findEmployeeIdsByDepartment(department);
        audit("getEmployeesByDepartment", null, input("department", department), result, true);
        return result;
    }

    @Tool(description = "查询员工的假期余额，包括年假、病假、婚假、产假等")
    public LeaveBalanceInfo getLeaveBalance(
            @ToolParam(description = "员工ID", required = true) String employeeId) {
        assertCanAccessEmployee(employeeId);
        LeaveBalanceInfo result = employeeBusinessService.findLeaveBalance(employeeId)
                .map(this::toLeaveBalanceInfo)
                .orElse(null);
        audit("getLeaveBalance", employeeId, input("employeeId", employeeId), result, result != null);
        return result;
    }

    @Tool(description = "申请请假，返回申请结果和申请ID")
    public LeaveApplicationResult applyLeave(
            @ToolParam(description = "员工ID", required = true) String employeeId,
            @ToolParam(description = "请假类型：ANNUAL（年假）、SICK（病假）、MARRIAGE（婚假）、MATERNITY（产假）、PERSONAL（事假）", required = true) String leaveType,
            @ToolParam(description = "开始日期，格式为 yyyy-MM-dd", required = true) String startDate,
            @ToolParam(description = "结束日期，格式为 yyyy-MM-dd", required = true) String endDate,
            @ToolParam(description = "请假原因", required = true) String reason) {
        assertCanAccessEmployee(employeeId);
        LocalDate start = LocalDate.parse(startDate, DATE_FORMATTER);
        LocalDate end = LocalDate.parse(endDate, DATE_FORMATTER);
        EmployeeBusinessService.LeaveApplicationResult result =
                employeeBusinessService.applyLeave(employeeId, leaveType, start, end, reason);
        LeaveApplicationResult response = new LeaveApplicationResult(result.success(), result.message(), result.applicationId());
        audit("applyLeave", employeeId, input(
                "employeeId", employeeId,
                "leaveType", leaveType,
                "startDate", startDate,
                "endDate", endDate,
                "reason", reason
        ), response, response.isSuccess());
        return response;
    }

    @Tool(description = "查询请假申请状态")
    public LeaveApplicationStatus getLeaveApplicationStatus(
            @ToolParam(description = "请假申请ID", required = true) String applicationId) {
        LeaveApplicationStatus result = employeeBusinessService.findLeaveApplication(applicationId)
                .map(this::toLeaveApplicationStatus)
                .orElse(null);
        if (result != null) {
            assertCanAccessEmployee(result.getEmployeeId());
        }
        audit("getLeaveApplicationStatus", result == null ? null : result.getEmployeeId(),
                input("applicationId", applicationId), result, result != null);
        return result;
    }

    @Tool(description = "查询员工的所有请假申请")
    public List<LeaveApplicationStatus> getEmployeeLeaveApplications(
            @ToolParam(description = "员工ID", required = true) String employeeId) {
        assertCanAccessEmployee(employeeId);
        List<LeaveApplicationStatus> result = employeeBusinessService.findLeaveApplicationsByEmployee(employeeId).stream()
                .map(this::toLeaveApplicationStatus)
                .toList();
        audit("getEmployeeLeaveApplications", employeeId, input("employeeId", employeeId), result, true);
        return result;
    }

    @Tool(description = "申请报销，返回申请结果和申请ID")
    public ReimbursementResult applyReimbursement(
            @ToolParam(description = "员工ID", required = true) String employeeId,
            @ToolParam(description = "报销类型：TRAVEL（差旅费）、MEAL（餐费）、TRANSPORT（交通费）、OFFICE（办公用品）、OTHER（其他）", required = true) String type,
            @ToolParam(description = "报销金额（元）", required = true) double amount,
            @ToolParam(description = "报销说明", required = true) String description,
            @ToolParam(description = "发票号码", required = false) String invoiceNumber) {
        assertCanAccessEmployee(employeeId);
        EmployeeBusinessService.ReimbursementResult result =
                employeeBusinessService.applyReimbursement(employeeId, type, amount, description, invoiceNumber);
        ReimbursementResult response = new ReimbursementResult(result.success(), result.message(), result.applicationId());
        audit("applyReimbursement", employeeId, input(
                "employeeId", employeeId,
                "type", type,
                "amount", amount,
                "description", description,
                "invoiceNumber", invoiceNumber
        ), response, response.isSuccess());
        return response;
    }

    @Tool(description = "查询报销申请状态")
    public ReimbursementStatus getReimbursementStatus(
            @ToolParam(description = "报销申请ID", required = true) String applicationId) {
        ReimbursementStatus result = employeeBusinessService.findReimbursementApplication(applicationId)
                .map(this::toReimbursementStatus)
                .orElse(null);
        if (result != null) {
            assertCanAccessEmployee(result.getEmployeeId());
        }
        audit("getReimbursementStatus", result == null ? null : result.getEmployeeId(),
                input("applicationId", applicationId), result, result != null);
        return result;
    }

    @Tool(description = "获取所有部门列表")
    public List<String> getAllDepartments() {
        List<String> result = employeeBusinessService.findAllDepartments();
        audit("getAllDepartments", null, Map.of(), result, true);
        return result;
    }

    private void audit(String toolName, String employeeId, Object input, Object result, boolean success) {
        toolAuditService.record(toolName, "employee-service", employeeId, input, result, success);
    }

    private void assertCanAccessEmployee(String employeeId) {
        CurrentUser user = CurrentUserContext.require();
        if (!user.canAccessEmployee(employeeId)) {
            throw new ForbiddenException("当前账号无权访问员工 " + employeeId + " 的数据");
        }
    }

    private void assertHrOrAdmin() {
        CurrentUser user = CurrentUserContext.require();
        if (!user.isHr() && !user.isAdmin()) {
            throw new ForbiddenException("当前账号无权查询员工列表");
        }
    }

    private Map<String, Object> input(Object... keyValues) {
        Map<String, Object> input = new LinkedHashMap<>();
        for (int index = 0; index + 1 < keyValues.length; index += 2) {
            input.put(String.valueOf(keyValues[index]), keyValues[index + 1]);
        }
        return input;
    }

    private EmployeeInfo toEmployeeInfo(EmployeeEntity employee) {
        return new EmployeeInfo(
                employee.getEmployeeId(),
                employee.getName(),
                employee.getDepartment(),
                employee.getPosition(),
                employee.getEmail(),
                employee.getPhone(),
                employee.getJoinDate().format(DATE_FORMATTER)
        );
    }

    private LeaveBalanceInfo toLeaveBalanceInfo(LeaveBalanceEntity balance) {
        return new LeaveBalanceInfo(
                balance.getEmployeeId(),
                balance.getAnnualLeave(),
                balance.getSickLeave(),
                balance.getMarriageLeave(),
                balance.getMaternityLeave()
        );
    }

    private LeaveApplicationStatus toLeaveApplicationStatus(LeaveApplicationEntity application) {
        return new LeaveApplicationStatus(
                application.getApplicationId(),
                application.getEmployeeId(),
                application.getLeaveType(),
                application.getStartDate().format(DATE_FORMATTER),
                application.getEndDate().format(DATE_FORMATTER),
                application.getDays(),
                application.getReason(),
                application.getStatus(),
                application.getApplyDate().format(DATE_FORMATTER)
        );
    }

    private ReimbursementStatus toReimbursementStatus(ReimbursementApplicationEntity application) {
        return new ReimbursementStatus(
                application.getApplicationId(),
                application.getEmployeeId(),
                application.getType(),
                application.getAmount().doubleValue(),
                application.getDescription(),
                application.getInvoiceNumber(),
                application.getStatus(),
                application.getApplyDate().format(DATE_FORMATTER)
        );
    }

    @Data
    public static class EmployeeInfo {
        private String employeeId;
        private String name;
        private String department;
        private String position;
        private String email;
        private String phone;
        private String joinDate;

        public EmployeeInfo(String employeeId, String name, String department, String position,
                            String email, String phone, String joinDate) {
            this.employeeId = employeeId;
            this.name = name;
            this.department = department;
            this.position = position;
            this.email = email;
            this.phone = phone;
            this.joinDate = joinDate;
        }
    }

    @Data
    public static class LeaveBalanceInfo {
        private String employeeId;
        private int annualLeave;
        private int sickLeave;
        private int marriageLeave;
        private int maternityLeave;

        public LeaveBalanceInfo(String employeeId, int annualLeave, int sickLeave,
                                int marriageLeave, int maternityLeave) {
            this.employeeId = employeeId;
            this.annualLeave = annualLeave;
            this.sickLeave = sickLeave;
            this.marriageLeave = marriageLeave;
            this.maternityLeave = maternityLeave;
        }
    }

    @Data
    public static class LeaveApplicationResult {
        private boolean success;
        private String message;
        private String applicationId;

        public LeaveApplicationResult(boolean success, String message, String applicationId) {
            this.success = success;
            this.message = message;
            this.applicationId = applicationId;
        }
    }

    @Data
    public static class LeaveApplicationStatus {
        private String applicationId;
        private String employeeId;
        private String leaveType;
        private String startDate;
        private String endDate;
        private int days;
        private String reason;
        private String status;
        private String applyDate;

        public LeaveApplicationStatus(String applicationId, String employeeId, String leaveType,
                                      String startDate, String endDate, int days, String reason,
                                      String status, String applyDate) {
            this.applicationId = applicationId;
            this.employeeId = employeeId;
            this.leaveType = leaveType;
            this.startDate = startDate;
            this.endDate = endDate;
            this.days = days;
            this.reason = reason;
            this.status = status;
            this.applyDate = applyDate;
        }
    }

    @Data
    public static class ReimbursementResult {
        private boolean success;
        private String message;
        private String applicationId;

        public ReimbursementResult(boolean success, String message, String applicationId) {
            this.success = success;
            this.message = message;
            this.applicationId = applicationId;
        }
    }

    @Data
    public static class ReimbursementStatus {
        private String applicationId;
        private String employeeId;
        private String type;
        private double amount;
        private String description;
        private String invoiceNumber;
        private String status;
        private String applyDate;

        public ReimbursementStatus(String applicationId, String employeeId, String type,
                                   double amount, String description, String invoiceNumber,
                                   String status, String applyDate) {
            this.applicationId = applicationId;
            this.employeeId = employeeId;
            this.type = type;
            this.amount = amount;
            this.description = description;
            this.invoiceNumber = invoiceNumber;
            this.status = status;
            this.applyDate = applyDate;
        }
    }
}
