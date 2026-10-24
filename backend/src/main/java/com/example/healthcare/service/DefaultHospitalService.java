package com.example.healthcare.service;

import com.example.healthcare.dto.hospital.HospitalCreateRequest;
import com.example.healthcare.dto.hospital.HospitalResponse;
import com.example.healthcare.dto.hospital.HospitalUpdateRequest;
import com.example.healthcare.entity.Hospital;
import com.example.healthcare.exception.DuplicateHospitalCodeException;
import com.example.healthcare.exception.HospitalNotFoundException;
import com.example.healthcare.repository.HospitalRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default implementation of {@link HospitalService} managing hospital lifecycle operations.
 */
@Service
public class DefaultHospitalService implements HospitalService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultHospitalService.class);

    private final HospitalRepository hospitalRepository;
    private final com.example.healthcare.security.TenantSecurityService tenantSecurityService;
    private final AuditService auditService;

    @org.springframework.beans.factory.annotation.Autowired
    public DefaultHospitalService(HospitalRepository hospitalRepository,
                                  com.example.healthcare.security.TenantSecurityService tenantSecurityService,
                                  AuditService auditService) {
        this.hospitalRepository = hospitalRepository;
        this.tenantSecurityService = tenantSecurityService;
        this.auditService = auditService;
    }

    public DefaultHospitalService(HospitalRepository hospitalRepository,
                                  com.example.healthcare.security.TenantSecurityService tenantSecurityService) {
        this(hospitalRepository, tenantSecurityService, null);
    }

    @Override
    @Transactional
    public HospitalResponse createHospital(HospitalCreateRequest request) {
        if (tenantSecurityService.getCurrentUser().isPresent() && !tenantSecurityService.isPlatformAdmin()) {
            logger.warn("Non-platform admin attempted to create a hospital");
            throw new com.example.healthcare.exception.TenantAccessDeniedException("Only platform administrators can register new hospitals");
        }

        String trimmedCode = request.getCode().trim().toUpperCase();
        String trimmedName = request.getName().trim();

        if (hospitalRepository.existsByCode(trimmedCode)) {
            throw new DuplicateHospitalCodeException("Hospital code '" + trimmedCode + "' is already in use");
        }

        Hospital hospital = new Hospital();
        hospital.setName(trimmedName);
        hospital.setCode(trimmedCode);
        hospital.setEmail(request.getEmail() != null ? request.getEmail().trim() : null);
        hospital.setPhone(request.getPhone() != null ? request.getPhone().trim() : null);
        hospital.setAddress(request.getAddress() != null ? request.getAddress().trim() : null);
        hospital.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);

        Hospital saved = hospitalRepository.save(hospital);
        logger.info("Hospital '{}' ({}) created successfully with ID {}", saved.getName(), saved.getCode(), saved.getId());

        if (auditService != null) {
            auditService.log(
                    "HOSPITAL_CREATE",
                    "HOSPITAL",
                    String.valueOf(saved.getId()),
                    "SUCCESS",
                    "Created hospital '" + saved.getName() + "' (Code: " + saved.getCode() + ")",
                    saved
            );
        }

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HospitalResponse> getAllHospitals() {
        if (tenantSecurityService.isPlatformAdmin()) {
            return hospitalRepository.findAllByOrderByNameAsc().stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }

        // Hospital-scoped users can only see their own hospital tenant
        Hospital hospital = tenantSecurityService.requireCurrentHospital();
        return List.of(mapToResponse(hospital));
    }

    @Override
    @Transactional(readOnly = true)
    public HospitalResponse getHospitalById(Long id) {
        tenantSecurityService.validateHospitalAccess(id);

        Hospital hospital = hospitalRepository.findById(id)
                .orElseThrow(() -> new HospitalNotFoundException("Hospital not found with id: " + id));
        return mapToResponse(hospital);
    }

    @Override
    @Transactional
    public HospitalResponse updateHospital(Long id, HospitalUpdateRequest request) {
        tenantSecurityService.validateHospitalAccess(id);

        Hospital hospital = hospitalRepository.findById(id)
                .orElseThrow(() -> new HospitalNotFoundException("Hospital not found with id: " + id));

        String trimmedCode = request.getCode().trim().toUpperCase();
        String trimmedName = request.getName().trim();

        if (hospitalRepository.existsByCodeAndIdNot(trimmedCode, id)) {
            throw new DuplicateHospitalCodeException("Hospital code '" + trimmedCode + "' is already in use");
        }

        hospital.setName(trimmedName);
        hospital.setCode(trimmedCode);
        hospital.setEmail(request.getEmail() != null ? request.getEmail().trim() : null);
        hospital.setPhone(request.getPhone() != null ? request.getPhone().trim() : null);
        hospital.setAddress(request.getAddress() != null ? request.getAddress().trim() : null);

        if (request.getEnabled() != null) {
            hospital.setEnabled(request.getEnabled());
        }

        Hospital updated = hospitalRepository.save(hospital);
        logger.info("Hospital ID {} updated successfully: '{}' ({})", updated.getId(), updated.getName(), updated.getCode());

        if (auditService != null) {
            auditService.log(
                    "HOSPITAL_UPDATE",
                    "HOSPITAL",
                    String.valueOf(updated.getId()),
                    "SUCCESS",
                    "Updated hospital '" + updated.getName() + "' (Code: " + updated.getCode() + ", enabled: " + updated.isEnabled() + ")",
                    updated
            );
        }

        return mapToResponse(updated);
    }

    private HospitalResponse mapToResponse(Hospital hospital) {
        return new HospitalResponse(
                hospital.getId(),
                hospital.getName(),
                hospital.getCode(),
                hospital.getEmail(),
                hospital.getPhone(),
                hospital.getAddress(),
                hospital.isEnabled(),
                hospital.getCreatedAt(),
                hospital.getUpdatedAt()
        );
    }
}
