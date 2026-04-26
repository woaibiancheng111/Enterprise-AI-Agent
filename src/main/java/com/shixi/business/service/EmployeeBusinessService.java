package com.shixi.business.service;

import com.shixi.business.entity.EmployeeEntity;
import com.shixi.business.entity.LeaveApplicationEntity;
import com.shixi.business.entity.LeaveBalanceEntity;
import com.shixi.business.entity.ReimbursementApplicationEntity;
import com.shixi.business.repository.EmployeeRepository;
import com.shixi.business.repository.LeaveApplicationRepository;
import com.shixi.business.repository.LeaveBalanceRepository;
import com.shixi.business.repository.ReimbursementApplicationRepository;
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

    private final EmployeeRepository employeeRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveApplicationRepository leaveApplicationRepository;
    private final ReimbursementApplicationRepository reimbursementApplicationRepository;

    public EmployeeBusinessService(
            EmployeeRepository employeeRepository,
            LeaveBalanceRepository leaveBalanceRepository,
            LeaveApplicationRepository leaveApplicationRepository,
            ReimbursementApplicationRepository reimbursementApplicationRepository) {
        this.employeeRepository = employeeRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.leaveApplicationRepository = leaveApplicationRepository;
        this.reimbursementApplicationRepository = reimbursementApplicationRepository;
    }

    @Transactional(readOnly = true)
    public Optional<EmployeeEntity> findEmployee(String employeeId) {
        return employeeRepository.findById(normalizeEmployeeId(employeeId));
    }

    @Transactional(readOnly = true)
    public List<String> findEmployeeIdsByName(String name) {
        return employeeRepository.findByName(name).stream()
                .map(EmployeeEntity::getEmployeeId)
                .sorted()
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> findEmployeeIdsByDepartment(String department) {
        return employeeRepository.findByDepartment(department).stream()
                .map(EmployeeEntity::getEmployeeId)
                .sorted()
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<LeaveBalanceEntity> findLeaveBalance(String employeeId) {
        return leaveBalanceRepository.findById(normalizeEmployeeId(employeeId));
    }

    @Transactional
    public LeaveApplicationResult applyLeave(String employeeId, String leaveType,
                                             LocalDate startDate, LocalDate endDate, String reason) {
        String normalizedEmployeeId = normalizeEmployeeId(employeeId);
        if (!employeeRepository.existsById(normalizedEmployeeId)) {
            return new LeaveApplicationResult(false, "员工不存在", null);
        }
        if (startDate.isAfter(endDate)) {
            return new LeaveApplicationResult(false, "开始日期不能晚于结束日期", null);
        }

        int days = (int) (endDate.toEpochDay() - startDate.toEpochDay()) + 1;
        String normalizedLeaveType = normalizeCode(leaveType);
        if (!"PERSONAL".equals(normalizedLeaveType)) {
            Optional<LeaveBalanceEntity> balance = leaveBalanceRepository.findById(normalizedEmployeeId);
            if (balance.isPresent()) {
                int availableDays = availableDays(balance.get(), normalizedLeaveType);
                if (days > availableDays) {
                    return new LeaveApplicationResult(false,
                            "假期余额不足，可用" + availableDays + "天，申请" + days + "天", null);
                }
            }
        }

        String applicationId = nextApplicationId("L", leaveApplicationRepository.findTopByOrderByApplicationIdDesc()
                .map(LeaveApplicationEntity::getApplicationId));
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
        leaveApplicationRepository.save(application);
        return new LeaveApplicationResult(true, "申请已提交，请等待审批", applicationId);
    }

    @Transactional(readOnly = true)
    public Optional<LeaveApplicationEntity> findLeaveApplication(String applicationId) {
        return leaveApplicationRepository.findById(normalizeCode(applicationId));
    }

    @Transactional(readOnly = true)
    public List<LeaveApplicationEntity> findLeaveApplicationsByEmployee(String employeeId) {
        return leaveApplicationRepository.findByEmployeeIdOrderByApplyDateDescApplicationIdDesc(normalizeEmployeeId(employeeId));
    }

    @Transactional
    public ReimbursementResult applyReimbursement(String employeeId, String type, double amount,
                                                  String description, String invoiceNumber) {
        String normalizedEmployeeId = normalizeEmployeeId(employeeId);
        if (!employeeRepository.existsById(normalizedEmployeeId)) {
            return new ReimbursementResult(false, "员工不存在", null);
        }
        if (amount <= 0) {
            return new ReimbursementResult(false, "报销金额必须大于0", null);
        }

        String applicationId = nextApplicationId("R", reimbursementApplicationRepository.findTopByOrderByApplicationIdDesc()
                .map(ReimbursementApplicationEntity::getApplicationId));
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
        reimbursementApplicationRepository.save(application);
        return new ReimbursementResult(true, "报销申请已提交，请等待审批", applicationId);
    }

    @Transactional(readOnly = true)
    public Optional<ReimbursementApplicationEntity> findReimbursementApplication(String applicationId) {
        return reimbursementApplicationRepository.findById(normalizeCode(applicationId));
    }

    @Transactional(readOnly = true)
    public List<String> findAllDepartments() {
        return employeeRepository.findAll().stream()
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

    private String nextApplicationId(String prefix, Optional<String> latestApplicationId) {
        int next = latestApplicationId
                .map(id -> id.replaceAll("\\D", ""))
                .filter(value -> !value.isBlank())
                .map(Integer::parseInt)
                .orElse(0) + 1;
        return prefix + String.format("%04d", next);
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
