package com.example.healthcare.repository;

import com.example.healthcare.entity.AuditLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link AuditLogEntity} records.
 * Supports pagination, specification filtering, and tenant-scoped lookups.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long>, JpaSpecificationExecutor<AuditLogEntity> {

    Page<AuditLogEntity> findByHospital_Id(Long hospitalId, Pageable pageable);

    List<AuditLogEntity> findByHospital_IdOrderByTimestampDesc(Long hospitalId);

    List<AuditLogEntity> findAllByOrderByTimestampDesc();
}
