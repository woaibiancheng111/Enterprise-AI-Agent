package com.shixi.business.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "reimbursement_applications")
public class ReimbursementApplicationEntity {

    @Id
    @Column(name = "application_id", length = 32)
    private String applicationId;

    @Column(name = "employee_id", nullable = false, length = 32)
    private String employeeId;

    @Column(nullable = false, length = 32)
    private String type;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 512)
    private String description;

    @Column(name = "invoice_number", length = 128)
    private String invoiceNumber;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "apply_date", nullable = false)
    private LocalDate applyDate;

    protected ReimbursementApplicationEntity() {
    }

    public ReimbursementApplicationEntity(String applicationId, String employeeId, String type,
                                          BigDecimal amount, String description, String invoiceNumber,
                                          String status, LocalDate applyDate) {
        this.applicationId = applicationId;
        this.employeeId = employeeId;
        this.type = type;
        this.amount = amount;
        this.description = description;
        this.invoiceNumber = invoiceNumber;
        this.status = status;
        this.applyDate = applyDate;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public String getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public String getStatus() {
        return status;
    }

    public LocalDate getApplyDate() {
        return applyDate;
    }
}
