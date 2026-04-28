package com.shixi.business.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("reimbursement_applications")
public class ReimbursementApplicationEntity {

    @TableId
    private String applicationId;

    private String employeeId;

    private String type;

    private BigDecimal amount;

    private String description;

    private String invoiceNumber;

    private String status;

    private LocalDate applyDate;
}
