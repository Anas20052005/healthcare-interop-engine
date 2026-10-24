package com.example.healthcare.service;

import com.example.healthcare.dto.hospital.HospitalCreateRequest;
import com.example.healthcare.dto.hospital.HospitalResponse;
import com.example.healthcare.dto.hospital.HospitalUpdateRequest;
import com.example.healthcare.entity.Hospital;
import com.example.healthcare.exception.DuplicateHospitalCodeException;
import com.example.healthcare.exception.HospitalNotFoundException;
import com.example.healthcare.repository.HospitalRepository;
import com.example.healthcare.security.TenantSecurityService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HospitalServiceTest {

    @Mock
    private HospitalRepository hospitalRepository;

    @Mock
    private TenantSecurityService tenantSecurityService;

    private DefaultHospitalService hospitalService;

    @BeforeEach
    void setUp() {
        hospitalService = new DefaultHospitalService(hospitalRepository, tenantSecurityService);
    }

    @Test
    @DisplayName("createHospital should save entity and return response DTO")
    void shouldCreateHospitalSuccessfully() {
        HospitalCreateRequest request = new HospitalCreateRequest(
                "St. Jude Medical Center",
                "stjude_01",
                "admin@stjude.org",
                "555-1234",
                "777 Care Blvd",
                true
        );

        when(hospitalRepository.existsByCode("STJUDE_01")).thenReturn(false);

        Hospital savedEntity = new Hospital("St. Jude Medical Center", "STJUDE_01", "admin@stjude.org", "555-1234", "777 Care Blvd", true);
        savedEntity.setId(1L);
        savedEntity.setCreatedAt(LocalDateTime.now());
        when(hospitalRepository.save(any(Hospital.class))).thenReturn(savedEntity);

        HospitalResponse response = hospitalService.createHospital(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("St. Jude Medical Center", response.getName());
        assertEquals("STJUDE_01", response.getCode());
        assertEquals("admin@stjude.org", response.getEmail());
        assertTrue(response.isEnabled());

        ArgumentCaptor<Hospital> captor = ArgumentCaptor.forClass(Hospital.class);
        verify(hospitalRepository).save(captor.capture());
        Hospital persisted = captor.getValue();
        assertEquals("St. Jude Medical Center", persisted.getName());
        assertEquals("STJUDE_01", persisted.getCode());
        assertTrue(persisted.isEnabled());
    }

    @Test
    @DisplayName("createHospital should throw DuplicateHospitalCodeException when code exists")
    void shouldThrowDuplicateCodeOnCreate() {
        HospitalCreateRequest request = new HospitalCreateRequest("General Hospital", "GEN_HOSP");

        when(hospitalRepository.existsByCode("GEN_HOSP")).thenReturn(true);

        DuplicateHospitalCodeException ex = assertThrows(
                DuplicateHospitalCodeException.class,
                () -> hospitalService.createHospital(request)
        );

        assertTrue(ex.getMessage().contains("GEN_HOSP"));
        verify(hospitalRepository, never()).save(any(Hospital.class));
    }

    @Test
    @DisplayName("getAllHospitals should return list of hospitals for platform admin")
    void shouldGetAllHospitals() {
        Hospital h1 = new Hospital("Apollo", "APOLLO");
        h1.setId(1L);
        Hospital h2 = new Hospital("Fortis", "FORTIS");
        h2.setId(2L);

        when(tenantSecurityService.isPlatformAdmin()).thenReturn(true);
        when(hospitalRepository.findAllByOrderByNameAsc()).thenReturn(List.of(h1, h2));

        List<HospitalResponse> list = hospitalService.getAllHospitals();

        assertEquals(2, list.size());
        assertEquals("Apollo", list.get(0).getName());
        assertEquals("Fortis", list.get(1).getName());
    }

    @Test
    @DisplayName("getHospitalById should return hospital response when found")
    void shouldGetHospitalById() {
        Hospital h = new Hospital("Apollo", "APOLLO");
        h.setId(1L);

        when(hospitalRepository.findById(1L)).thenReturn(Optional.of(h));

        HospitalResponse response = hospitalService.getHospitalById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Apollo", response.getName());
        verify(tenantSecurityService).validateHospitalAccess(1L);
    }

    @Test
    @DisplayName("getHospitalById should throw HospitalNotFoundException when not found")
    void shouldThrowWhenHospitalNotFoundById() {
        when(hospitalRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(HospitalNotFoundException.class, () -> hospitalService.getHospitalById(99L));
    }

    @Test
    @DisplayName("updateHospital should update fields and return response")
    void shouldUpdateHospitalSuccessfully() {
        Hospital existing = new Hospital("Old Name", "OLD_CODE");
        existing.setId(5L);

        HospitalUpdateRequest updateRequest = new HospitalUpdateRequest(
                "New Name",
                "NEW_CODE",
                "updated@test.com",
                "555-9999",
                "Updated Address",
                false
        );

        when(hospitalRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(hospitalRepository.existsByCodeAndIdNot("NEW_CODE", 5L)).thenReturn(false);

        Hospital updatedEntity = new Hospital("New Name", "NEW_CODE", "updated@test.com", "555-9999", "Updated Address", false);
        updatedEntity.setId(5L);
        when(hospitalRepository.save(any(Hospital.class))).thenReturn(updatedEntity);

        HospitalResponse response = hospitalService.updateHospital(5L, updateRequest);

        assertNotNull(response);
        assertEquals("New Name", response.getName());
        assertEquals("NEW_CODE", response.getCode());
        assertFalse(response.isEnabled());
    }

    @Test
    @DisplayName("updateHospital should throw DuplicateHospitalCodeException when code belongs to another hospital")
    void shouldThrowDuplicateCodeOnUpdate() {
        Hospital existing = new Hospital("Hospital One", "HOSP_ONE");
        existing.setId(5L);

        HospitalUpdateRequest updateRequest = new HospitalUpdateRequest("Hospital One", "TAKEN_CODE");

        when(hospitalRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(hospitalRepository.existsByCodeAndIdNot("TAKEN_CODE", 5L)).thenReturn(true);

        assertThrows(DuplicateHospitalCodeException.class, () -> hospitalService.updateHospital(5L, updateRequest));
        verify(hospitalRepository, never()).save(any(Hospital.class));
    }
}
