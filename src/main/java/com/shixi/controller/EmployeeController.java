package com.shixi.controller;

import com.shixi.business.entity.EmployeeEntity;
import com.shixi.business.entity.LeaveBalanceEntity;
import com.shixi.business.service.EmployeeBusinessService;
import com.shixi.security.CurrentUser;
import com.shixi.security.CurrentUserContext;
import com.shixi.security.ForbiddenException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("employees")
public class EmployeeController {

    private final EmployeeBusinessService employeeBusinessService;

    public EmployeeController(EmployeeBusinessService employeeBusinessService) {
        this.employeeBusinessService = employeeBusinessService;
    }

    @GetMapping
    public EmployeeListResponse employees(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String department) {
        CurrentUser user = CurrentUserContext.require();
        if (!user.isHr() && !user.isAdmin()) {
            throw new ForbiddenException("当前账号无权访问员工列表");
        }
        List<EmployeeCard> employees = employeeBusinessService.listEmployees(keyword, department).stream()
                .map(this::toEmployeeCard)
                .toList();
        return new EmployeeListResponse(true, employees.size(), employees);
    }

    @GetMapping("/me/overview")
    public EmployeeOverviewResponse myOverview() {
        CurrentUser user = CurrentUserContext.require();
        if (user.employeeId() == null || user.employeeId().isBlank()) {
            return new EmployeeOverviewResponse(
                    true,
                    null,
                    null,
                    List.of(),
                    Map.of("pending", 0, "approved", 0, "rejected", 0, "total", 0)
            );
        }
        return overview(user.employeeId());
    }

    @GetMapping("/{employeeId}/overview")
    public EmployeeOverviewResponse employeeOverview(@PathVariable String employeeId) {
        CurrentUser user = CurrentUserContext.require();
        if (!user.canAccessEmployee(employeeId)) {
            throw new ForbiddenException("当前账号无权访问该员工视图");
        }
        return overview(employeeId);
    }

    private EmployeeOverviewResponse overview(String employeeId) {
        EmployeeCard employee = employeeBusinessService.findEmployee(employeeId)
                .map(this::toEmployeeCard)
                .orElseThrow(() -> new IllegalArgumentException("员工不存在"));
        LeaveBalance balance = employeeBusinessService.findLeaveBalance(employeeId)
                .map(this::toLeaveBalance)
                .orElse(null);
        List<EmployeeBusinessService.WorkflowApplication> applications =
                employeeBusinessService.listWorkflowApplicationsByEmployee(employeeId);
        long pending = applications.stream().filter(item -> "PENDING".equals(item.status())).count();
        long approved = applications.stream().filter(item -> "APPROVED".equals(item.status())).count();
        long rejected = applications.stream().filter(item -> "REJECTED".equals(item.status())).count();
        return new EmployeeOverviewResponse(
                true,
                employee,
                balance,
                applications,
                Map.of(
                        "pending", pending,
                        "approved", approved,
                        "rejected", rejected,
                        "total", applications.size()
                )
        );
    }

    private EmployeeCard toEmployeeCard(EmployeeEntity employee) {
        return new EmployeeCard(
                employee.getEmployeeId(),
                employee.getName(),
                employee.getDepartment(),
                employee.getPosition(),
                employee.getEmail(),
                employee.getPhone(),
                employee.getJoinDate()
        );
    }

    private LeaveBalance toLeaveBalance(LeaveBalanceEntity balance) {
        return new LeaveBalance(
                balance.getEmployeeId(),
                balance.getAnnualLeave(),
                balance.getSickLeave(),
                balance.getMarriageLeave(),
                balance.getMaternityLeave()
        );
    }

    public record EmployeeListResponse(boolean success, int count, List<EmployeeCard> employees) {
    }

    public record EmployeeOverviewResponse(
            boolean success,
            EmployeeCard employee,
            LeaveBalance leaveBalance,
            List<EmployeeBusinessService.WorkflowApplication> applications,
            Map<String, ? extends Number> summary) {
    }

    public record EmployeeCard(
            String employeeId,
            String name,
            String department,
            String position,
            String email,
            String phone,
            LocalDate joinDate) {
    }

    public record LeaveBalance(
            String employeeId,
            int annualLeave,
            int sickLeave,
            int marriageLeave,
            int maternityLeave) {
    }
}
