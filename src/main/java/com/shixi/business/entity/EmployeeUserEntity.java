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
@TableName("employee_users")
public class EmployeeUserEntity {

    @TableId
    private String userId;

    private String username;

    private String passwordHash;

    private String employeeId;

    private String displayName;

    private String role;

    private boolean enabled;

    private LocalDateTime createdAt;
}
