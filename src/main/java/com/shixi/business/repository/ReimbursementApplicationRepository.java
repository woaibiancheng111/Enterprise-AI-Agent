package com.shixi.business.repository;

import com.shixi.business.entity.ReimbursementApplicationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReimbursementApplicationRepository extends JpaRepository<ReimbursementApplicationEntity, String> {

    Optional<ReimbursementApplicationEntity> findTopByOrderByApplicationIdDesc();
}
