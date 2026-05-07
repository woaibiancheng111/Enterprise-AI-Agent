package com.shixi.agent;

import com.shixi.mcp.EmployeeServiceTools;
import com.shixi.mcp.KnowledgeBaseTools;
import com.shixi.mcp.TimeTools;
import com.shixi.security.CurrentUser;
import com.shixi.security.CurrentUserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ToolOrchestrationServiceTest {

    private final TimeTools timeTools = mock(TimeTools.class);
    private final EmployeeServiceTools employeeServiceTools = mock(EmployeeServiceTools.class);
    private final KnowledgeBaseTools knowledgeBaseTools = mock(KnowledgeBaseTools.class);
    private final ToolOrchestrationService service =
            new ToolOrchestrationService(timeTools, employeeServiceTools, knowledgeBaseTools);

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void handlesLeaveBalanceWithDeterministicToolPath() {
        when(employeeServiceTools.getLeaveBalance("E001"))
                .thenReturn(new EmployeeServiceTools.LeaveBalanceInfo("E001", 8, 5, 10, 0));

        Optional<String> answer = service.tryHandle("查询员工 E001 的年假余额");

        assertTrue(answer.isPresent());
        assertTrue(answer.get().contains("getLeaveBalance"));
        assertTrue(answer.get().contains("年假：8 天"));
    }

    @Test
    void resolvesCurrentEmployeeForSelfServiceLeaveBalance() {
        CurrentUserContext.set(new CurrentUser("U001", "zhangsan", "E001", "张三", "EMPLOYEE"));
        when(employeeServiceTools.getLeaveBalance("E001"))
                .thenReturn(new EmployeeServiceTools.LeaveBalanceInfo("E001", 15, 10, 5, 3));

        Optional<String> answer = service.tryHandle("查一下我的假期");

        assertTrue(answer.isPresent());
        assertTrue(answer.get().contains("员工 `E001`"));
        assertTrue(answer.get().contains("年假：15 天"));
    }

    @Test
    void resolvesEmployeeNameForManagerToolRequest() {
        when(employeeServiceTools.findEmployeeIdsByName("张三")).thenReturn(List.of("E001"));
        when(employeeServiceTools.getLeaveBalance("E001"))
                .thenReturn(new EmployeeServiceTools.LeaveBalanceInfo("E001", 15, 10, 5, 3));

        Optional<String> answer = service.tryHandle("帮我查询张三的假期余额");

        assertTrue(answer.isPresent());
        assertTrue(answer.get().contains("getLeaveBalance"));
        assertTrue(answer.get().contains("员工 `E001`"));
    }

    @Test
    void asksForAmountWhenReimbursementIntentLacksMoney() {
        Optional<String> answer = service.tryHandle("帮员工 E001 申请交通费报销");

        assertTrue(answer.isPresent());
        assertTrue(answer.get().contains("缺少有效金额"));
    }

    @Test
    void handlesReimbursementWhenAmountIsPresent() {
        when(employeeServiceTools.applyReimbursement("E001", "TRANSPORT", 80.0, "由智能工具调用根据用户描述创建", null))
                .thenReturn(new EmployeeServiceTools.ReimbursementResult(true, "报销申请已提交", "R0001"));

        Optional<String> answer = service.tryHandle("帮员工 E001 申请交通费报销 80 元");

        assertTrue(answer.isPresent());
        assertTrue(answer.get().contains("applyReimbursement"));
        assertTrue(answer.get().contains("R0001"));
    }

    @Test
    void handlesTimeQuestionWithoutLlm() {
        when(timeTools.getCurrentDate()).thenReturn("2026-05-06");
        when(timeTools.getCurrentDateTime()).thenReturn("2026-05-06 15:30:00");
        when(timeTools.getDayOfWeek("2026-05-06")).thenReturn("星期三");
        when(timeTools.calculateWorkDaysBetween("2026-05-06", "2026-05-31")).thenReturn(18);

        Optional<String> answer = service.tryHandle("今天是星期几，到月底还有多少工作日");

        assertTrue(answer.isPresent());
        assertTrue(answer.get().contains("2026-05-06 15:30:00"));
        assertTrue(answer.get().contains("18 个工作日"));
    }
}
