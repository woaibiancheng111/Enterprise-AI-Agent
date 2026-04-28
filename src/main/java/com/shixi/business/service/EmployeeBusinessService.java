package com.shixi.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shixi.business.entity.EmployeeEntity;
import com.shixi.business.entity.LeaveApplicationEntity;
import com.shixi.business.entity.LeaveBalanceEntity;
import com.shixi.business.entity.ReimbursementApplicationEntity;
import com.shixi.business.mapper.EmployeeMapper;
import com.shixi.business.mapper.LeaveApplicationMapper;
import com.shixi.business.mapper.LeaveBalanceMapper;
import com.shixi.business.mapper.ReimbursementApplicationMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class EmployeeBusinessService {

    private final EmployeeMapper employeeMapper;
    private final LeaveBalanceMapper leaveBalanceMapper;
    private final LeaveApplicationMapper leaveApplicationMapper;
    private final ReimbursementApplicationMapper reimbursementApplicationMapper;

    public EmployeeBusinessService(
            EmployeeMapper employeeMapper,
            LeaveBalanceMapper leaveBalanceMapper,
            LeaveApplicationMapper leaveApplicationMapper,
            ReimbursementApplicationMapper reimbursementApplicationMapper) {
        this.employeeMapper = employeeMapper;
        this.leaveBalanceMapper = leaveBalanceMapper;
        this.leaveApplicationMapper = leaveApplicationMapper;
        this.reimbursementApplicationMapper = reimbursementApplicationMapper;
    }

    @Transactional(readOnly = true)
    public Optional<EmployeeEntity> findEmployee(String employeeId) {
        return Optional.ofNullable(employeeMapper.selectById(normalizeEmployeeId(employeeId)));
    }

    @Transactional(readOnly = true)
    public List<String> findEmployeeIdsByName(String name) {
        return employeeMapper.selectList(new LambdaQueryWrapper<EmployeeEntity>()
                        .eq(EmployeeEntity::getName, name))
                .stream()
                .map(EmployeeEntity::getEmployeeId)
                .sorted()
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> findEmployeeIdsByDepartment(String department) {
        return employeeMapper.selectList(new LambdaQueryWrapper<EmployeeEntity>()
                        .eq(EmployeeEntity::getDepartment, department))
                .stream()
                .map(EmployeeEntity::getEmployeeId)
                .sorted()
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<LeaveBalanceEntity> findLeaveBalance(String employeeId) {
        return Optional.ofNullable(leaveBalanceMapper.selectById(normalizeEmployeeId(employeeId)));
    }

    @Transactional
    public LeaveApplicationResult applyLeave(String employeeId, String leaveType,
                                             LocalDate startDate, LocalDate endDate, String reason) {
        String normalizedEmployeeId = normalizeEmployeeId(employeeId);
        if (employeeMapper.selectById(normalizedEmployeeId) == null) {
            return new LeaveApplicationResult(false, "员工不存在", null);
        }
        if (startDate.isAfter(endDate)) {
            return new LeaveApplicationResult(false, "开始日期不能晚于结束日期", null);
        }

        int days = (int) (endDate.toEpochDay() - startDate.toEpochDay()) + 1;
        String normalizedLeaveType = normalizeCode(leaveType);
        if (!"PERSONAL".equals(normalizedLeaveType)) {
            LeaveBalanceEntity balance = leaveBalanceMapper.selectById(normalizedEmployeeId);
            if (balance != null) {
                int availableDays = availableDays(balance, normalizedLeaveType);
                if (days > availableDays) {
                    return new LeaveApplicationResult(false,
                            "假期余额不足，可用" + availableDays + "天，申请" + days + "天", null);
                }
            }
        }

        String applicationId = nextApplicationId("L");
        LeaveApplicationEntity application = new LeaveApplicationEntity(
                applicationId,
                normalizedEmployeeId,
                normalizedLeaveType,
                startDate,
                endDate,
                days,
                reason,
                "PENDING",
                LocalDate.now()
        );
        leaveApplicationMapper.insert(application);
        return new LeaveApplicationResult(true, "申请已提交，请等待审批", applicationId);
    }

    @Transactional(readOnly = true)
    public Optional<LeaveApplicationEntity> findLeaveApplication(String applicationId) {
        return Optional.ofNullable(leaveApplicationMapper.selectById(normalizeCode(applicationId)));
    }

    @Transactional(readOnly = true)
    public List<LeaveApplicationEntity> findLeaveApplicationsByEmployee(String employeeId) {
        return leaveApplicationMapper.selectList(new LambdaQueryWrapper<LeaveApplicationEntity>()
                .eq(LeaveApplicationEntity::getEmployeeId, normalizeEmployeeId(employeeId))
                .orderByDesc(LeaveApplicationEntity::getApplyDate)
                .orderByDesc(LeaveApplicationEntity::getApplicationId));
    }

    @Transactional
    public ReimbursementResult applyReimbursement(String employeeId, String type, double amount,
                                                  String description, String invoiceNumber) {
        String normalizedEmployeeId = normalizeEmployeeId(employeeId);
        if (employeeMapper.selectById(normalizedEmployeeId) == null) {
            return new ReimbursementResult(false, "员工不存在", null);
        }
        if (amount <= 0) {
            return new ReimbursementResult(false, "报销金额必须大于0", null);
        }

        String applicationId = nextApplicationId("R");
        ReimbursementApplicationEntity application = new ReimbursementApplicationEntity(
                applicationId,
                normalizedEmployeeId,
                normalizeCode(type),
                BigDecimal.valueOf(amount),
                description,
                invoiceNumber,
                "PENDING",
                LocalDate.now()
        );
        reimbursementApplicationMapper.insert(application);
        return new ReimbursementResult(true, "报销申请已提交，请等待审批", applicationId);
    }

    @Transactional(readOnly = true)
    public Optional<ReimbursementApplicationEntity> findReimbursementApplication(String applicationId) {
        return Optional.ofNullable(reimbursementApplicationMapper.selectById(normalizeCode(applicationId)));
    }

    @Transactional(readOnly = true)
    public List<String> findAllDepartments() {
        return employeeMapper.selectList(null).stream()
                .map(EmployeeEntity::getDepartment)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    private int availableDays(LeaveBalanceEntity balance, String leaveType) {
        return switch (leaveType) {
            case "ANNUAL" -> balance.getAnnualLeave();
            case "SICK" -> balance.getSickLeave();
            case "MARRIAGE" -> balance.getMarriageLeave();
            case "MATERNITY" -> balance.getMaternityLeave();
            default -> Integer.MAX_VALUE;
        };
    }

    private String nextApplicationId(String prefix) {
        List<String> applicationIds = "L".equals(prefix)
                ? selectLeaveApplicationIds(prefix)
                : selectReimbursementApplicationIds(prefix);
        int next = applicationIds.stream()
                .map(id -> id.replaceAll("\\D", ""))
                .filter(value -> !value.isBlank())
                .mapToInt(Integer::parseInt)
                .max()
                .orElse(0) + 1;
        return prefix + String.format("%04d", next);
    }

    private List<String> selectLeaveApplicationIds(String prefix) {
        return leaveApplicationMapper.selectList(new QueryWrapper<LeaveApplicationEntity>()
                        .select("application_id")
                        .likeRight("application_id", prefix))
                .stream()
                .map(LeaveApplicationEntity::getApplicationId)
                .toList();
    }

    private List<String> selectReimbursementApplicationIds(String prefix) {
        return reimbursementApplicationMapper.selectList(new QueryWrapper<ReimbursementApplicationEntity>()
                        .select("application_id")
                        .likeRight("application_id", prefix))
                .stream()
                .map(ReimbursementApplicationEntity::getApplicationId)
                .toList();
    }

    private String normalizeEmployeeId(String employeeId) {
        return normalizeCode(employeeId);
    }

    private String normalizeCode(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    public record LeaveApplicationResult(boolean success, String message, String applicationId) {
    }

    public record ReimbursementResult(boolean success, String message, String applicationId) {
    }
}
