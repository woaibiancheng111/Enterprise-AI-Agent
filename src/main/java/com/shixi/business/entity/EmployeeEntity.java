package com.shixi.business.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "employees")
public class EmployeeEntity {

    @Id
    @Column(name = "employee_id", length = 32)
    private String employeeId;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(nullable = false, length = 64)
    private String department;

    @Column(nullable = false, length = 128)
    private String position;

    @Column(nullable = false, length = 128)
    private String email;

    @Column(nullable = false, length = 32)
    private String phone;

    @Column(name = "join_date", nullable = false)
    private LocalDate joinDate;

    protected EmployeeEntity() {
    }

    public EmployeeEntity(String employeeId, String name, String department, String position,
                          String email, String phone, LocalDate joinDate) {
        this.employeeId = employeeId;
        this.name = name;
        this.department = department;
        this.position = position;
        this.email = email;
        this.phone = phone;
        this.joinDate = joinDate;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public String getName() {
        return name;
    }

    public String getDepartment() {
        return department;
    }

    public String getPosition() {
        return position;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public LocalDate getJoinDate() {
        return joinDate;
    }
}
