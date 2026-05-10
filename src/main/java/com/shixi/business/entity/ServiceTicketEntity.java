package com.shixi.business.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("service_tickets")
public class ServiceTicketEntity {

    @TableId
    private String ticketId;

    private String employeeId;

    private String employeeName;

    private String department;

    private String requirementType;

    private String title;

    private String description;

    private String priority;

    private String status;

    private String assigneeGroup;

    private String sla;

    private String requiredFields;

    private String actionItems;

    private String submittedBy;

    private LocalDateTime submittedAt;

    private LocalDateTime createdAt;
}
