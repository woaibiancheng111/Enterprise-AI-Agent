package com.shixi.mcp;

import lombok.Data;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class EmployeeServiceTools {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    private final Map<String, Employee> employeeDatabase = new ConcurrentHashMap<>();
    private final Map<String, LeaveBalance> leaveBalanceDatabase = new ConcurrentHashMap<>();
    private final Map<String, LeaveApplication> leaveApplications = new ConcurrentHashMap<>();
    private final Map<String, ReimbursementApplication> reimbursementApplications = new ConcurrentHashMap<>();
    
    private final AtomicLong leaveIdCounter = new AtomicLong(1);
    private final AtomicLong reimbursementIdCounter = new AtomicLong(1);

    public EmployeeServiceTools() {
        initMockData();
    }

    private void initMockData() {
        employeeDatabase.put("E001", new Employee("E001", "张三", "技术部", "高级工程师", "zhangsan@company.com", "13800138001", LocalDate.of(2020, 3, 15)));
        employeeDatabase.put("E002", new Employee("E002", "李四", "人事部", "HR专员", "lisi@company.com", "13800138002", LocalDate.of(2021, 6, 20)));
        employeeDatabase.put("E003", new Employee("E003", "王五", "财务部", "会计", "wangwu@company.com", "13800138003", LocalDate.of(2019, 9, 10)));
        employeeDatabase.put("E004", new Employee("E004", "赵六", "市场部", "市场经理", "zhaoliu@company.com", "13800138004", LocalDate.of(2018, 2, 28)));
        employeeDatabase.put("E005", new Employee("E005", "钱七", "技术部", "初级工程师", "qianqi@company.com", "13800138005", LocalDate.of(2023, 7, 1)));

        leaveBalanceDatabase.put("E001", new LeaveBalance("E001", 15, 10, 5, 3));
        leaveBalanceDatabase.put("E002", new LeaveBalance("E002", 12, 8, 3, 2));
        leaveBalanceDatabase.put("E003", new LeaveBalance("E003", 18, 12, 6, 4));
        leaveBalanceDatabase.put("E004", new LeaveBalance("E004", 20, 15, 8, 5));
        leaveBalanceDatabase.put("E005", new LeaveBalance("E005", 5, 3, 1, 0));
    }

    @Tool(description = "根据员工ID查询员工基本信息")
    public EmployeeInfo getEmployeeInfo(
            @ToolParam(description = "员工ID，如 E001", required = true) String employeeId) {
        Employee employee = employeeDatabase.get(employeeId);
        if (employee == null) {
            return null;
        }
        return new EmployeeInfo(
                employee.employeeId(),
                employee.name(),
                employee.department(),
                employee.position(),
                employee.email(),
                employee.phone(),
                employee.joinDate().format(DATE_FORMATTER)
        );
    }

    @Tool(description = "根据员工姓名查询员工ID列表（可能有重名）")
    public List<String> findEmployeeIdsByName(
            @ToolParam(description = "员工姓名", required = true) String name) {
        List<String> ids = new ArrayList<>();
        for (Map.Entry<String, Employee> entry : employeeDatabase.entrySet()) {
            if (entry.getValue().name().equals(name)) {
                ids.add(entry.getKey());
            }
        }
        return ids;
    }

    @Tool(description = "查询指定部门的所有员工ID列表")
    public List<String> getEmployeesByDepartment(
            @ToolParam(description = "部门名称，如 技术部、人事部", required = true) String department) {
        List<String> ids = new ArrayList<>();
        for (Map.Entry<String, Employee> entry : employeeDatabase.entrySet()) {
            if (entry.getValue().department().equals(department)) {
                ids.add(entry.getKey());
            }
        }
        return ids;
    }

    @Tool(description = "查询员工的假期余额，包括年假、病假、婚假、产假等")
    public LeaveBalanceInfo getLeaveBalance(
            @ToolParam(description = "员工ID", required = true) String employeeId) {
        LeaveBalance balance = leaveBalanceDatabase.get(employeeId);
        if (balance == null) {
            return null;
        }
        return new LeaveBalanceInfo(
                balance.employeeId(),
                balance.annualLeave(),
                balance.sickLeave(),
                balance.marriageLeave(),
                balance.maternityLeave()
        );
    }

    @Tool(description = "申请请假，返回申请结果和申请ID")
    public LeaveApplicationResult applyLeave(
            @ToolParam(description = "员工ID", required = true) String employeeId,
            @ToolParam(description = "请假类型：ANNUAL（年假）、SICK（病假）、MARRIAGE（婚假）、MATERNITY（产假）、PERSONAL（事假）", required = true) String leaveType,
            @ToolParam(description = "开始日期，格式为 yyyy-MM-dd", required = true) String startDate,
            @ToolParam(description = "结束日期，格式为 yyyy-MM-dd", required = true) String endDate,
            @ToolParam(description = "请假原因", required = true) String reason) {
        
        if (!employeeDatabase.containsKey(employeeId)) {
            return new LeaveApplicationResult(false, "员工不存在", null);
        }

        LocalDate start = LocalDate.parse(startDate, DATE_FORMATTER);
        LocalDate end = LocalDate.parse(endDate, DATE_FORMATTER);
        
        if (start.isAfter(end)) {
            return new LeaveApplicationResult(false, "开始日期不能晚于结束日期", null);
        }

        int days = (int) (end.toEpochDay() - start.toEpochDay()) + 1;

        LeaveBalance balance = leaveBalanceDatabase.get(employeeId);
        if (balance != null && !"PERSONAL".equals(leaveType)) {
            int availableDays = switch (leaveType) {
                case "ANNUAL" -> balance.annualLeave();
                case "SICK" -> balance.sickLeave();
                case "MARRIAGE" -> balance.marriageLeave();
                case "MATERNITY" -> balance.maternityLeave();
                default -> Integer.MAX_VALUE;
            };
            if (days > availableDays) {
                return new LeaveApplicationResult(false, "假期余额不足，可用" + availableDays + "天，申请" + days + "天", null);
            }
        }

        String applicationId = "L" + String.format("%04d", leaveIdCounter.getAndIncrement());
        LeaveApplication application = new LeaveApplication(
                applicationId,
                employeeId,
                leaveType,
                startDate,
                endDate,
                days,
                reason,
                "PENDING",
                LocalDate.now().format(DATE_FORMATTER)
        );
        leaveApplications.put(applicationId, application);

        return new LeaveApplicationResult(true, "申请已提交，请等待审批", applicationId);
    }

    @Tool(description = "查询请假申请状态")
    public LeaveApplicationStatus getLeaveApplicationStatus(
            @ToolParam(description = "请假申请ID", required = true) String applicationId) {
        LeaveApplication application = leaveApplications.get(applicationId);
        if (application == null) {
            return null;
        }
        return new LeaveApplicationStatus(
                application.applicationId(),
                application.employeeId(),
                application.leaveType(),
                application.startDate(),
                application.endDate(),
                application.days(),
                application.reason(),
                application.status(),
                application.applyDate()
        );
    }

    @Tool(description = "查询员工的所有请假申请")
    public List<LeaveApplicationStatus> getEmployeeLeaveApplications(
            @ToolParam(description = "员工ID", required = true) String employeeId) {
        List<LeaveApplicationStatus> applications = new ArrayList<>();
        for (LeaveApplication app : leaveApplications.values()) {
            if (app.employeeId().equals(employeeId)) {
                applications.add(new LeaveApplicationStatus(
                        app.applicationId(),
                        app.employeeId(),
                        app.leaveType(),
                        app.startDate(),
                        app.endDate(),
                        app.days(),
                        app.reason(),
                        app.status(),
                        app.applyDate()
                ));
            }
        }
        return applications;
    }

    @Tool(description = "申请报销，返回申请结果和申请ID")
    public ReimbursementResult applyReimbursement(
            @ToolParam(description = "员工ID", required = true) String employeeId,
            @ToolParam(description = "报销类型：TRAVEL（差旅费）、MEAL（餐费）、TRANSPORT（交通费）、OFFICE（办公用品）、OTHER（其他）", required = true) String type,
            @ToolParam(description = "报销金额（元）", required = true) double amount,
            @ToolParam(description = "报销说明", required = true) String description,
            @ToolParam(description = "发票号码", required = false) String invoiceNumber) {
        
        if (!employeeDatabase.containsKey(employeeId)) {
            return new ReimbursementResult(false, "员工不存在", null);
        }

        if (amount <= 0) {
            return new ReimbursementResult(false, "报销金额必须大于0", null);
        }

        String applicationId = "R" + String.format("%04d", reimbursementIdCounter.getAndIncrement());
        ReimbursementApplication application = new ReimbursementApplication(
                applicationId,
                employeeId,
                type,
                amount,
                description,
                invoiceNumber,
                "PENDING",
                LocalDate.now().format(DATE_FORMATTER)
        );
        reimbursementApplications.put(applicationId, application);

        return new ReimbursementResult(true, "报销申请已提交，请等待审批", applicationId);
    }

    @Tool(description = "查询报销申请状态")
    public ReimbursementStatus getReimbursementStatus(
            @ToolParam(description = "报销申请ID", required = true) String applicationId) {
        ReimbursementApplication application = reimbursementApplications.get(applicationId);
        if (application == null) {
            return null;
        }
        return new ReimbursementStatus(
                application.applicationId(),
                application.employeeId(),
                application.type(),
                application.amount(),
                application.description(),
                application.invoiceNumber(),
                application.status(),
                application.applyDate()
        );
    }

    @Tool(description = "获取所有部门列表")
    public List<String> getAllDepartments() {
        Set<String> departments = new HashSet<>();
        for (Employee employee : employeeDatabase.values()) {
            departments.add(employee.department());
        }
        return new ArrayList<>(departments);
    }

    public record Employee(
            String employeeId,
            String name,
            String department,
            String position,
            String email,
            String phone,
            LocalDate joinDate
    ) {}

    public record LeaveBalance(
            String employeeId,
            int annualLeave,
            int sickLeave,
            int marriageLeave,
            int maternityLeave
    ) {}

    public record LeaveApplication(
            String applicationId,
            String employeeId,
            String leaveType,
            String startDate,
            String endDate,
            int days,
            String reason,
            String status,
            String applyDate
    ) {}

    public record ReimbursementApplication(
            String applicationId,
            String employeeId,
            String type,
            double amount,
            String description,
            String invoiceNumber,
            String status,
            String applyDate
    ) {}

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
