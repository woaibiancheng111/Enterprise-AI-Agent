package com.shixi.business.repository;

import com.shixi.business.entity.LeaveApplicationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeaveApplicationRepository extends JpaRepository<LeaveApplicationEntity, String> {

    List<LeaveApplicationEntity> findByEmployeeIdOrderByApplyDateDescApplicationIdDesc(String employeeId);

    Optional<LeaveApplicationEntity> findTopByOrderByApplicationIdDesc();
}
