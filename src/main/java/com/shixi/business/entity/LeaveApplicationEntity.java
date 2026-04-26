package com.shixi.business.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "leave_applications")
public class LeaveApplicationEntity {

    @Id
    @Column(name = "application_id", length = 32)
    private String applicationId;

    @Column(name = "employee_id", nullable = false, length = 32)
    private String employeeId;

    @Column(name = "leave_type", nullable = false, length = 32)
    private String leaveType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private int days;

    @Column(nullable = false, length = 512)
    private String reason;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "apply_date", nullable = false)
    private LocalDate applyDate;

    protected LeaveApplicationEntity() {
    }

    public LeaveApplicationEntity(String applicationId, String employeeId, String leaveType,
                                  LocalDate startDate, LocalDate endDate, int days,
                                  String reason, String status, LocalDate applyDate) {
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

    public String getApplicationId() {
        return applicationId;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public String getLeaveType() {
        return leaveType;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public int getDays() {
        return days;
    }

    public String getReason() {
        return reason;
    }

    public String getStatus() {
        return status;
    }

    public LocalDate getApplyDate() {
        return applyDate;
    }
}
