package com.shixi.business.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "leave_balances")
public class LeaveBalanceEntity {

    @Id
    @Column(name = "employee_id", length = 32)
    private String employeeId;

    @Column(name = "annual_leave", nullable = false)
    private int annualLeave;

    @Column(name = "sick_leave", nullable = false)
    private int sickLeave;

    @Column(name = "marriage_leave", nullable = false)
    private int marriageLeave;

    @Column(name = "maternity_leave", nullable = false)
    private int maternityLeave;

    protected LeaveBalanceEntity() {
    }

    public LeaveBalanceEntity(String employeeId, int annualLeave, int sickLeave,
                              int marriageLeave, int maternityLeave) {
        this.employeeId = employeeId;
        this.annualLeave = annualLeave;
        this.sickLeave = sickLeave;
        this.marriageLeave = marriageLeave;
        this.maternityLeave = maternityLeave;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public int getAnnualLeave() {
        return annualLeave;
    }

    public int getSickLeave() {
        return sickLeave;
    }

    public int getMarriageLeave() {
        return marriageLeave;
    }

    public int getMaternityLeave() {
        return maternityLeave;
    }
}
