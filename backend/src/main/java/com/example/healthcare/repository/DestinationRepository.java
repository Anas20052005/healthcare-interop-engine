package com.example.healthcare.repository;

import com.example.healthcare.entity.DestinationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link DestinationEntity} configurations.
 */
@Repository
public interface DestinationRepository extends JpaRepository<DestinationEntity, Long> {

    List<DestinationEntity> findAllByOrderByCreatedAtDesc();

    List<DestinationEntity> findByHospitalIdOrHospitalIsNullOrderByCreatedAtDesc(Long hospitalId);

    List<DestinationEntity> findByHospitalIdOrderByCreatedAtDesc(Long hospitalId);

    List<DestinationEntity> findByHospitalIsNullOrderByCreatedAtDesc();

    Optional<DestinationEntity> findByIdAndHospitalId(Long id, Long hospitalId);

    boolean existsByNameAndHospitalId(String name, Long hospitalId);

    boolean existsByNameAndHospitalIsNull(String name);

    boolean existsByMockTrue();

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(d) FROM DestinationEntity d WHERE d.hospital.id = :hospitalId OR d.hospital IS NULL")
    long countAccessibleByHospitalId(@org.springframework.data.repository.query.Param("hospitalId") Long hospitalId);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(d) FROM DestinationEntity d WHERE (d.hospital.id = :hospitalId OR d.hospital IS NULL) AND d.enabled = true")
    long countAccessibleAndEnabledByHospitalId(@org.springframework.data.repository.query.Param("hospitalId") Long hospitalId);

    long countByEnabledTrue();
}
