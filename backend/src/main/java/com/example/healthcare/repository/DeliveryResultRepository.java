package com.example.healthcare.repository;

import com.example.healthcare.entity.DeliveryResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Data JPA repository for {@link DeliveryResultEntity} audit records and retry tracking.
 */
@Repository
public interface DeliveryResultRepository extends JpaRepository<DeliveryResultEntity, Long> {

    List<DeliveryResultEntity> findByConversionIdOrderByAttemptTimestampDesc(Long conversionId);

    List<DeliveryResultEntity> findByDestinationIdOrderByAttemptTimestampDesc(Long destinationId);

    List<DeliveryResultEntity> findByHospitalIdOrderByAttemptTimestampDesc(Long hospitalId);

    List<DeliveryResultEntity> findAllByOrderByAttemptTimestampDesc();

    /**
     * Finds deliveries in RETRYING status that are due for automatic execution.
     */
    List<DeliveryResultEntity> findByStatusAndNextRetryAtLessThanEqual(String status, LocalDateTime now);

    long countByHospitalId(Long hospitalId);

    long countByHospitalIdAndStatus(Long hospitalId, String status);

    List<DeliveryResultEntity> findTop5ByHospitalIdOrderByAttemptTimestampDesc(Long hospitalId);

    /**
     * Atomically claims a due delivery for automatic retry to prevent race conditions across concurrent workers.
     *
     * @param id  the delivery record ID
     * @param now current timestamp for updated_at
     * @return 1 if successfully claimed, 0 if already claimed or not in RETRYING state
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE DeliveryResultEntity d SET d.status = 'IN_PROGRESS', d.updatedAt = :now WHERE d.id = :id AND d.status = 'RETRYING'")
    int claimForRetry(@Param("id") Long id, @Param("now") LocalDateTime now);

    /**
     * Atomically claims an eligible failed delivery for manual retry.
     *
     * @param id  the delivery record ID
     * @param now current timestamp for updated_at
     * @return 1 if successfully claimed, 0 if not in FAILED state or already claimed
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE DeliveryResultEntity d SET d.status = 'IN_PROGRESS', d.updatedAt = :now WHERE d.id = :id AND d.status = 'FAILED'")
    int claimForManualRetry(@Param("id") Long id, @Param("now") LocalDateTime now);

    /**
     * Releases or updates an IN_PROGRESS delivery to a terminal or retry status.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE DeliveryResultEntity d SET d.status = :newStatus, d.updatedAt = :now WHERE d.id = :id AND d.status = 'IN_PROGRESS'")
    int releaseClaim(@Param("id") Long id, @Param("newStatus") String newStatus, @Param("now") LocalDateTime now);
}
