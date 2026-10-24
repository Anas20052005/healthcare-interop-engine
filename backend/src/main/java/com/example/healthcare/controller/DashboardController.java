package com.example.healthcare.controller;

import com.example.healthcare.dto.dashboard.DashboardStatsResponse;
import com.example.healthcare.dto.delivery.DeliveryResultResponse;
import com.example.healthcare.dto.hl7.ConversionHistoryResponse;
import com.example.healthcare.entity.ConversionEntity;
import com.example.healthcare.entity.DeliveryResultEntity;
import com.example.healthcare.entity.Hospital;
import com.example.healthcare.repository.ConversionRepository;
import com.example.healthcare.repository.DeliveryResultRepository;
import com.example.healthcare.repository.DestinationRepository;
import com.example.healthcare.repository.EncounterRepository;
import com.example.healthcare.repository.ObservationRepository;
import com.example.healthcare.repository.PatientRepository;
import com.example.healthcare.security.TenantSecurityService;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for dashboard statistics.
 * Computes analytics strictly scoped to the authenticated user's hospital tenant
 * or platform-wide for authenticated platform administrators.
 */
@RestController
@RequestMapping("/api/dashboard")
@Transactional(readOnly = true)
public class DashboardController {

    private final ConversionRepository conversionRepository;
    private final PatientRepository patientRepository;
    private final EncounterRepository encounterRepository;
    private final ObservationRepository observationRepository;
    private final DeliveryResultRepository deliveryResultRepository;
    private final DestinationRepository destinationRepository;
    private final TenantSecurityService tenantSecurityService;
    private final DataSource dataSource;

    public DashboardController(ConversionRepository conversionRepository,
                               PatientRepository patientRepository,
                               EncounterRepository encounterRepository,
                               ObservationRepository observationRepository,
                               DeliveryResultRepository deliveryResultRepository,
                               DestinationRepository destinationRepository,
                               TenantSecurityService tenantSecurityService,
                               DataSource dataSource) {
        this.conversionRepository = conversionRepository;
        this.patientRepository = patientRepository;
        this.encounterRepository = encounterRepository;
        this.observationRepository = observationRepository;
        this.deliveryResultRepository = deliveryResultRepository;
        this.destinationRepository = destinationRepository;
        this.tenantSecurityService = tenantSecurityService;
        this.dataSource = dataSource;
    }

    /**
     * Retrieves aggregated metrics, clinical totals, delivery statuses, destination counts,
     * engine health status, and recent activity strictly scoped to the authenticated hospital tenant.
     */
    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsResponse> getStats() {
        Hospital hospital = tenantSecurityService.requireCurrentHospital();
        Long hospitalId = hospital.getId();

        // 1. Conversions (database-derived)
        long totalConversions = conversionRepository.countByHospitalId(hospitalId);
        long successfulConversions = conversionRepository.countByHospitalIdAndStatus(hospitalId, "SUCCESS");
        long failedConversions = conversionRepository.countByHospitalIdAndStatus(hospitalId, "FAILED");

        // Safe rate calculation (0 if no conversions exist)
        double conversionSuccessRate = totalConversions > 0
                ? Math.round(((double) successfulConversions / totalConversions) * 1000.0) / 10.0
                : 0.0;

        // 2. Clinical Resources processed (unique per hospital)
        long patientsProcessed = patientRepository.countByHospitalId(hospitalId);
        long encountersProcessed = encounterRepository.countByHospitalId(hospitalId);
        long observationsProcessed = observationRepository.countByHospitalId(hospitalId);

        // 3. Deliveries (Priority 7 & Priority 9)
        long totalDeliveries = deliveryResultRepository.countByHospitalId(hospitalId);
        long successfulDeliveries = deliveryResultRepository.countByHospitalIdAndStatus(hospitalId, "SUCCESS");
        long failedDeliveries = deliveryResultRepository.countByHospitalIdAndStatus(hospitalId, "FAILED");
        long retryingDeliveries = deliveryResultRepository.countByHospitalIdAndStatus(hospitalId, "RETRYING");
        long pendingDeliveries = deliveryResultRepository.countByHospitalIdAndStatus(hospitalId, "IN_PROGRESS")
                + deliveryResultRepository.countByHospitalIdAndStatus(hospitalId, "PENDING");

        double deliverySuccessRate = totalDeliveries > 0
                ? Math.round(((double) successfulDeliveries / totalDeliveries) * 1000.0) / 10.0
                : 0.0;

        // 4. Destinations (Priority 5 & Priority 6 accessible to hospital)
        long totalDestinations = destinationRepository.countAccessibleByHospitalId(hospitalId);
        long enabledDestinations = destinationRepository.countAccessibleAndEnabledByHospitalId(hospitalId);

        // 5. Engine Health Probe
        boolean dbAlive = false;
        try (Connection conn = dataSource.getConnection()) {
            dbAlive = conn.isValid(2);
        } catch (Exception ignored) {
        }
        String databaseStatus = dbAlive ? "UP" : "DOWN";
        String engineStatus = dbAlive ? "OPERATIONAL" : "DEGRADED";

        // 6. Recent Activity
        List<ConversionHistoryResponse> recentConversions = conversionRepository
                .findTop5ByHospitalIdOrderByCreatedAtDesc(hospitalId).stream()
                .map(this::mapToHistoryResponse)
                .collect(Collectors.toList());

        List<DeliveryResultResponse> recentDeliveries = deliveryResultRepository
                .findTop5ByHospitalIdOrderByAttemptTimestampDesc(hospitalId).stream()
                .map(this::mapToDeliveryResponse)
                .collect(Collectors.toList());

        DashboardStatsResponse stats = new DashboardStatsResponse(
                hospitalId,
                hospital.getName(),
                hospital.getCode(),
                totalConversions,
                successfulConversions,
                failedConversions,
                conversionSuccessRate,
                patientsProcessed,
                encountersProcessed,
                observationsProcessed,
                totalDeliveries,
                successfulDeliveries,
                failedDeliveries,
                retryingDeliveries,
                pendingDeliveries,
                deliverySuccessRate,
                totalDestinations,
                enabledDestinations,
                engineStatus,
                databaseStatus,
                recentConversions,
                recentDeliveries
        );

        return ResponseEntity.ok(stats);
    }

    private ConversionHistoryResponse mapToHistoryResponse(ConversionEntity entity) {
        Long hospitalId = entity.getHospital() != null ? entity.getHospital().getId() : null;
        String hospitalName = entity.getHospital() != null ? entity.getHospital().getName() : null;

        return new ConversionHistoryResponse(
                entity.getId(),
                entity.getMessageControlId(),
                entity.getMessageType(),
                entity.getPatientIdentifier(),
                entity.getEncounterIdentifier(),
                entity.getObservationCount() != null ? entity.getObservationCount() : 0,
                entity.getStatus(),
                entity.getErrorMessage(),
                entity.getCreatedAt(),
                hospitalId,
                hospitalName
        );
    }

    private DeliveryResultResponse mapToDeliveryResponse(DeliveryResultEntity entity) {
        Long hospitalId = entity.getHospital() != null ? entity.getHospital().getId() : null;
        String hospitalName = entity.getHospital() != null ? entity.getHospital().getName() : null;

        return new DeliveryResultResponse(
                entity.getId(),
                entity.getConversionId(),
                entity.getDestinationId(),
                entity.getDestinationName(),
                entity.getAttemptTimestamp(),
                entity.getStatus(),
                entity.getHttpStatusCode(),
                entity.getErrorMessage(),
                entity.getResponseSummary(),
                entity.getAttemptCount(),
                entity.getMaxAttempts(),
                entity.getNextRetryAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.isManualRetryEligible(),
                hospitalId,
                hospitalName
        );
    }
}
