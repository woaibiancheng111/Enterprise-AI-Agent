package com.shixi.business.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("employees")
public class EmployeeEntity {

    @TableId
    private String employeeId;

    private String name;

    private String department;

    @TableField("job_position")
    private String position;

    private String email;

    private String phone;

    private LocalDate joinDate;
}
