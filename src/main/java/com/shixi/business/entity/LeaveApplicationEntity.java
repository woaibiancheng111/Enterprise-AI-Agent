package com.shixi.business.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("leave_applications")
public class LeaveApplicationEntity {

    @TableId
    private String applicationId;

    private String employeeId;

    private String leaveType;

    private LocalDate startDate;

    private LocalDate endDate;

    private int days;

    private String reason;

    private String status;

    private LocalDate applyDate;
}
