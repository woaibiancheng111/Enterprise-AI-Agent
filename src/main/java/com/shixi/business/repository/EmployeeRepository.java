package com.shixi.business.repository;

import com.shixi.business.entity.EmployeeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmployeeRepository extends JpaRepository<EmployeeEntity, String> {

    List<EmployeeEntity> findByName(String name);

    List<EmployeeEntity> findByDepartment(String department);
}
