package com.example.healthcare.repository;

import com.example.healthcare.entity.ConversionEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link ConversionEntity} audit and history persistence.
 */
@Repository
public interface ConversionRepository extends JpaRepository<ConversionEntity, Long> {

    Optional<ConversionEntity> findByMessageControlId(String messageControlId);

    List<ConversionEntity> findByStatus(String status);

    List<ConversionEntity> findAllByOrderByCreatedAtDesc();

    List<ConversionEntity> findAllByHospitalIdOrderByCreatedAtDesc(Long hospitalId);

    Optional<ConversionEntity> findByIdAndHospitalId(Long id, Long hospitalId);

    long countByHospitalId(Long hospitalId);

    long countByHospitalIdAndStatus(Long hospitalId, String status);

    List<ConversionEntity> findTop5ByHospitalIdOrderByCreatedAtDesc(Long hospitalId);
}
