package com.shixi.agent;

import com.shixi.mcp.EmployeeServiceTools;
import com.shixi.mcp.KnowledgeBaseTools;
import com.shixi.mcp.TimeTools;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 本地工具编排兜底。
 *
 * LLM tool calling 失败或不适合流式输出时，仍然能对常见企业工具请求
 * 做确定性解析和执行，避免智能工具调用入口返回 500。
 */
@Service
public class ToolOrchestrationService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Pattern EMPLOYEE_ID_PATTERN = Pattern.compile("\\bE\\d{3}\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern DATE_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final Pattern DAYS_PATTERN = Pattern.compile("(\\d+)\\s*天");
    private static final Pattern LEAVE_APPLICATION_PATTERN = Pattern.compile("[申请办理].*假|请假|年假|病假|事假|婚假|产假");
    private static final Pattern LEAVE_STATUS_PATTERN = Pattern.compile("\\bL\\d{4}\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern REIMBURSEMENT_STATUS_PATTERN = Pattern.compile("\\bR\\d{4}\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern MONEY_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:元|块|rmb|RMB)?");

    private final TimeTools timeTools;
    private final EmployeeServiceTools employeeServiceTools;
    private final KnowledgeBaseTools knowledgeBaseTools;

    public ToolOrchestrationService(
            TimeTools timeTools,
            EmployeeServiceTools employeeServiceTools,
            KnowledgeBaseTools knowledgeBaseTools) {
        this.timeTools = timeTools;
        this.employeeServiceTools = employeeServiceTools;
        this.knowledgeBaseTools = knowledgeBaseTools;
    }

    public Optional<String> tryHandle(String message) {
        if (message == null || message.isBlank()) {
            return Optional.empty();
        }

        String normalized = message.toLowerCase(Locale.ROOT);

        Optional<String> statusResult = tryHandleStatusQuery(message);
        if (statusResult.isPresent()) {
            return statusResult;
        }

        Optional<String> employeeId = extractEmployeeId(message);
        if (employeeId.isPresent() && containsAny(normalized, "假期余额", "年假余额", "剩几天", "还剩")) {
            return Optional.of(formatLeaveBalance(employeeId.get()));
        }

        if (employeeId.isPresent() && containsAny(normalized, "基本信息", "员工信息", "查询员工", "是谁", "邮箱", "电话")) {
            return Optional.of(formatEmployeeInfo(employeeId.get()));
        }

        if (employeeId.isPresent() && LEAVE_APPLICATION_PATTERN.matcher(message).find()
                && containsAny(normalized, "申请", "办理", "帮员工")) {
            return Optional.of(applyLeave(message, employeeId.get()));
        }

        if (employeeId.isPresent() && containsAny(normalized, "报销", "发票", "交通费", "差旅", "餐费")) {
            return Optional.of(applyReimbursement(message, employeeId.get()));
        }

        if (containsAny(normalized, "今天", "现在", "星期", "工作日", "月底", "日期")) {
            return Optional.of(formatTimeAnswer(message));
        }

        if (containsAny(normalized, "知识库", "制度", "政策", "流程", "规定", "报销", "请假")) {
            return Optional.of(searchKnowledge(message));
        }

        return Optional.empty();
    }

    private Optional<String> tryHandleStatusQuery(String message) {
        Matcher leaveMatcher = LEAVE_STATUS_PATTERN.matcher(message);
        if (leaveMatcher.find()) {
            String applicationId = leaveMatcher.group().toUpperCase(Locale.ROOT);
            EmployeeServiceTools.LeaveApplicationStatus status =
                    employeeServiceTools.getLeaveApplicationStatus(applicationId);
            if (status == null) {
                return Optional.of("已调用工具：`getLeaveApplicationStatus`\n\n未找到请假申请 `" + applicationId + "`。");
            }
            return Optional.of("""
                    已调用工具：`getLeaveApplicationStatus`

                    请假申请 `%s` 当前状态：**%s**
                    - 员工：%s
                    - 假期类型：%s
                    - 日期：%s 至 %s，共 %d 天
                    - 原因：%s
                    - 申请日期：%s
                    """.formatted(
                    status.getApplicationId(),
                    status.getStatus(),
                    status.getEmployeeId(),
                    status.getLeaveType(),
                    status.getStartDate(),
                    status.getEndDate(),
                    status.getDays(),
                    status.getReason(),
                    status.getApplyDate()
            ));
        }

        Matcher reimbursementMatcher = REIMBURSEMENT_STATUS_PATTERN.matcher(message);
        if (reimbursementMatcher.find()) {
            String applicationId = reimbursementMatcher.group().toUpperCase(Locale.ROOT);
            EmployeeServiceTools.ReimbursementStatus status =
                    employeeServiceTools.getReimbursementStatus(applicationId);
            if (status == null) {
                return Optional.of("已调用工具：`getReimbursementStatus`\n\n未找到报销申请 `" + applicationId + "`。");
            }
            return Optional.of("""
                    已调用工具：`getReimbursementStatus`

                    报销申请 `%s` 当前状态：**%s**
                    - 员工：%s
                    - 类型：%s
                    - 金额：%.2f 元
                    - 说明：%s
                    - 发票号：%s
                    - 申请日期：%s
                    """.formatted(
                    status.getApplicationId(),
                    status.getStatus(),
                    status.getEmployeeId(),
                    status.getType(),
                    status.getAmount(),
                    status.getDescription(),
                    status.getInvoiceNumber() == null ? "未填写" : status.getInvoiceNumber(),
                    status.getApplyDate()
            ));
        }

        return Optional.empty();
    }

    private String formatEmployeeInfo(String employeeId) {
        EmployeeServiceTools.EmployeeInfo info = employeeServiceTools.getEmployeeInfo(employeeId);
        if (info == null) {
            return "已调用工具：`getEmployeeInfo`\n\n未找到员工 `" + employeeId + "`。";
        }
        return """
                已调用工具：`getEmployeeInfo`

                员工 `%s` 的基本信息如下：
                - 姓名：%s
                - 部门：%s
                - 职位：%s
                - 邮箱：%s
                - 电话：%s
                - 入职日期：%s
                """.formatted(
                info.getEmployeeId(),
                info.getName(),
                info.getDepartment(),
                info.getPosition(),
                info.getEmail(),
                info.getPhone(),
                info.getJoinDate()
        );
    }

    private String formatLeaveBalance(String employeeId) {
        EmployeeServiceTools.LeaveBalanceInfo balance = employeeServiceTools.getLeaveBalance(employeeId);
        if (balance == null) {
            return "已调用工具：`getLeaveBalance`\n\n未找到员工 `" + employeeId + "` 的假期余额。";
        }
        return """
                已调用工具：`getLeaveBalance`

                员工 `%s` 的假期余额：
                - 年假：%d 天
                - 病假：%d 天
                - 婚假：%d 天
                - 产假：%d 天
                """.formatted(
                balance.getEmployeeId(),
                balance.getAnnualLeave(),
                balance.getSickLeave(),
                balance.getMarriageLeave(),
                balance.getMaternityLeave()
        );
    }

    private String applyLeave(String message, String employeeId) {
        String leaveType = detectLeaveType(message);
        String startDate = extractDate(message).orElse(timeTools.getCurrentDate());
        int days = extractDays(message).orElse(1);
        String endDate = LocalDate.parse(startDate, DATE_FORMATTER).plusDays(days - 1L).format(DATE_FORMATTER);

        EmployeeServiceTools.LeaveApplicationResult result = employeeServiceTools.applyLeave(
                employeeId,
                leaveType,
                startDate,
                endDate,
                "由智能工具调用根据用户描述创建"
        );

        if (!result.isSuccess()) {
            return """
                    已调用工具：`applyLeave`

                    请假申请未提交成功：%s
                    - 员工：%s
                    - 假期类型：%s
                    - 日期：%s 至 %s，共 %d 天
                    """.formatted(result.getMessage(), employeeId, leaveType, startDate, endDate, days);
        }

        return """
                已调用工具：`applyLeave`

                请假申请已提交成功。
                - 申请编号：%s
                - 员工：%s
                - 假期类型：%s
                - 日期：%s 至 %s，共 %d 天
                - 状态：待审批
                """.formatted(result.getApplicationId(), employeeId, leaveType, startDate, endDate, days);
    }

    private String applyReimbursement(String message, String employeeId) {
        String type = detectReimbursementType(message);
        double amount = extractMoney(message).orElse(0.0);
        if (amount <= 0) {
            return "已识别到报销工具调用意图，但缺少有效金额。请补充报销金额，例如：`帮员工 E001 申请交通费报销 80 元`。";
        }

        EmployeeServiceTools.ReimbursementResult result = employeeServiceTools.applyReimbursement(
                employeeId,
                type,
                amount,
                "由智能工具调用根据用户描述创建",
                null
        );

        if (!result.isSuccess()) {
            return """
                    已调用工具：`applyReimbursement`

                    报销申请未提交成功：%s
                    - 员工：%s
                    - 类型：%s
                    - 金额：%.2f 元
                    """.formatted(result.getMessage(), employeeId, type, amount);
        }

        return """
                已调用工具：`applyReimbursement`

                报销申请已提交成功。
                - 申请编号：%s
                - 员工：%s
                - 类型：%s
                - 金额：%.2f 元
                - 状态：待审批
                """.formatted(result.getApplicationId(), employeeId, type, amount);
    }

    private String formatTimeAnswer(String message) {
        String currentDate = timeTools.getCurrentDate();
        String currentDateTime = timeTools.getCurrentDateTime();
        String dayOfWeek = timeTools.getDayOfWeek(currentDate);
        LocalDate today = LocalDate.parse(currentDate, DATE_FORMATTER);
        LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());
        int workDaysToMonthEnd = timeTools.calculateWorkDaysBetween(currentDate, monthEnd.format(DATE_FORMATTER));

        return """
                已调用工具：`getCurrentDateTime`、`getDayOfWeek`、`calculateWorkDaysBetween`

                当前时间：%s
                今天是：%s，%s
                从今天到本月底（%s）共有 **%d 个工作日**。
                """.formatted(currentDateTime, currentDate, dayOfWeek, monthEnd.format(DATE_FORMATTER), workDaysToMonthEnd);
    }

    private String searchKnowledge(String message) {
        KnowledgeBaseTools.KnowledgeSearchResult result = knowledgeBaseTools.searchKnowledgeBase(message, 3);
        if (result.getDocuments().isEmpty()) {
            return "已调用工具：`searchKnowledgeBase`\n\n知识库暂未检索到相关内容。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("已调用工具：`searchKnowledgeBase`\n\n");
        sb.append("检索意图：").append(result.getIntentType()).append("\n");
        sb.append("改写查询：").append(result.getRewrittenQuery()).append("\n\n");
        sb.append("相关知识片段：\n");
        for (int i = 0; i < result.getDocuments().size(); i++) {
            KnowledgeBaseTools.DocumentInfo doc = result.getDocuments().get(i);
            sb.append(i + 1)
                    .append(". `")
                    .append(doc.getSourceFile())
                    .append("`：")
                    .append(truncate(doc.getHighlight(), 120))
                    .append("\n");
        }
        return sb.toString();
    }

    private Optional<String> extractEmployeeId(String message) {
        Matcher matcher = EMPLOYEE_ID_PATTERN.matcher(message);
        if (matcher.find()) {
            return Optional.of(matcher.group().toUpperCase(Locale.ROOT));
        }
        return Optional.empty();
    }

    private Optional<String> extractDate(String message) {
        Matcher matcher = DATE_PATTERN.matcher(message);
        if (matcher.find()) {
            return Optional.of(matcher.group());
        }
        return Optional.empty();
    }

    private Optional<Integer> extractDays(String message) {
        Matcher matcher = DAYS_PATTERN.matcher(message);
        if (matcher.find()) {
            return Optional.of(Integer.parseInt(matcher.group(1)));
        }
        return Optional.empty();
    }

    private Optional<Double> extractMoney(String message) {
        Matcher matcher = MONEY_PATTERN.matcher(message);
        while (matcher.find()) {
            double amount = Double.parseDouble(matcher.group(1));
            if (amount > 0) {
                return Optional.of(amount);
            }
        }
        return Optional.empty();
    }

    private String detectLeaveType(String message) {
        if (message.contains("病假")) return "SICK";
        if (message.contains("事假")) return "PERSONAL";
        if (message.contains("婚假")) return "MARRIAGE";
        if (message.contains("产假")) return "MATERNITY";
        return "ANNUAL";
    }

    private String detectReimbursementType(String message) {
        if (message.contains("差旅") || message.contains("出差")) return "TRAVEL";
        if (message.contains("餐")) return "MEAL";
        if (message.contains("交通") || message.contains("打车") || message.contains("车费")) return "TRANSPORT";
        if (message.contains("办公")) return "OFFICE";
        return "OTHER";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }
}
