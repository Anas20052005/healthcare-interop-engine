package com.example.healthcare.service;

import com.example.healthcare.dto.destination.DestinationCreateRequest;
import com.example.healthcare.dto.destination.DestinationResponse;
import com.example.healthcare.dto.destination.DestinationUpdateRequest;
import com.example.healthcare.entity.DestinationAuthType;
import com.example.healthcare.entity.DestinationEntity;
import com.example.healthcare.entity.Hospital;
import com.example.healthcare.exception.DestinationNotFoundException;
import com.example.healthcare.exception.InvalidDestinationException;
import com.example.healthcare.exception.TenantAccessDeniedException;
import com.example.healthcare.repository.DestinationRepository;
import com.example.healthcare.repository.HospitalRepository;
import com.example.healthcare.security.TenantSecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DestinationServiceTest {

    @Mock
    private DestinationRepository destinationRepository;

    @Mock
    private HospitalRepository hospitalRepository;

    @Mock
    private TenantSecurityService tenantSecurityService;

    @Mock
    private SsrfProtectionService ssrfProtectionService;

    @Mock
    private TokenCryptoService tokenCryptoService;

    @InjectMocks
    private DefaultDestinationService destinationService;

    private Hospital hospitalA;
    private Hospital hospitalB;
    private DestinationEntity destinationA;
    private DestinationEntity platformDestination;

    @BeforeEach
    void setUp() {
        hospitalA = new Hospital("Hospital Alpha", "HOSP_A");
        hospitalA.setId(1L);

        hospitalB = new Hospital("Hospital Beta", "HOSP_B");
        hospitalB.setId(2L);

        destinationA = new DestinationEntity("Alpha FHIR", "https://fhir.hospital-a.com/r4", "R4",
                DestinationAuthType.BEARER_TOKEN, true, false, hospitalA);
        destinationA.setId(10L);
        destinationA.setAuthToken("encrypted-token-123");

        platformDestination = new DestinationEntity("Shared Mock FHIR", "http://localhost:8080/api/mock-fhir", "R4",
                DestinationAuthType.NONE, true, true, null);
        platformDestination.setId(20L);
    }

    @Test
    @DisplayName("Hospital user creates hospital-scoped destination successfully with encrypted token")
    void testCreateDestinationSuccess() {
        when(tenantSecurityService.isPlatformAdmin()).thenReturn(false);
        when(tenantSecurityService.requireCurrentHospital()).thenReturn(hospitalA);
        doNothing().when(ssrfProtectionService).validateDestinationUrl(anyString(), anyBoolean());
        when(destinationRepository.existsByNameAndHospitalId("Alpha FHIR", 1L)).thenReturn(false);
        when(tokenCryptoService.encrypt("secret-token-abc")).thenReturn("encrypted-token-abc");
        when(destinationRepository.save(any(DestinationEntity.class))).thenAnswer(i -> {
            DestinationEntity saved = i.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        DestinationCreateRequest request = new DestinationCreateRequest(
                "Alpha FHIR", "https://fhir.hospital-a.com/r4", "R4",
                DestinationAuthType.BEARER_TOKEN, "secret-token-abc", true
        );

        DestinationResponse response = destinationService.createDestination(request);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals("Alpha FHIR", response.getName());
        assertEquals("https://fhir.hospital-a.com/r4", response.getBaseUrl());
        assertTrue(response.isHasAuthToken());
        assertEquals(1L, response.getHospitalId());
        assertFalse(response.isPlatformWide());
    }

    @Test
    @DisplayName("Create destination rejects invalid or SSRF private URL")
    void testCreateDestinationRejectsSsrf() {
        when(tenantSecurityService.isPlatformAdmin()).thenReturn(false);
        when(tenantSecurityService.requireCurrentHospital()).thenReturn(hospitalA);
        doThrow(new InvalidDestinationException("Destination URL points to a restricted local or private network address"))
                .when(ssrfProtectionService).validateDestinationUrl(eq("http://192.168.1.1/fhir"), anyBoolean());

        DestinationCreateRequest request = new DestinationCreateRequest(
                "Private Server", "http://192.168.1.1/fhir", "R4",
                DestinationAuthType.NONE, null, true
        );

        InvalidDestinationException ex = assertThrows(InvalidDestinationException.class,
                () -> destinationService.createDestination(request));
        assertTrue(ex.getMessage().contains("restricted"));
    }

    @Test
    @DisplayName("Listing destinations for hospital user returns hospital destinations and platform-wide destinations")
    void testListDestinationsHospitalUser() {
        when(tenantSecurityService.isPlatformAdmin()).thenReturn(false);
        when(tenantSecurityService.requireCurrentHospital()).thenReturn(hospitalA);
        when(destinationRepository.findByHospitalIdOrHospitalIsNullOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(destinationA, platformDestination));

        List<DestinationResponse> list = destinationService.getAccessibleDestinations();

        assertEquals(2, list.size());
        assertEquals("Alpha FHIR", list.get(0).getName());
        assertTrue(list.get(0).isHasAuthToken());
        assertEquals("Shared Mock FHIR", list.get(1).getName());
        assertTrue(list.get(1).isPlatformWide());
    }

    @Test
    @DisplayName("Hospital A user cannot access or view Hospital B's destination")
    void testGetDestinationCrossHospitalDenied() {
        DestinationEntity destB = new DestinationEntity("Beta FHIR", "https://fhir.hospital-b.com/r4", "R4",
                DestinationAuthType.NONE, true, false, hospitalB);
        destB.setId(30L);

        when(destinationRepository.findById(30L)).thenReturn(Optional.of(destB));
        when(tenantSecurityService.isPlatformAdmin()).thenReturn(false);
        when(tenantSecurityService.requireCurrentHospital()).thenReturn(hospitalA);

        assertThrows(TenantAccessDeniedException.class, () -> destinationService.getDestinationById(30L));
    }

    @Test
    @DisplayName("Updating destination retains existing token when token is blank")
    void testUpdateDestinationRetainsExistingToken() {
        when(destinationRepository.findById(10L)).thenReturn(Optional.of(destinationA));
        when(tenantSecurityService.isPlatformAdmin()).thenReturn(false);
        when(tenantSecurityService.requireCurrentHospital()).thenReturn(hospitalA);
        doNothing().when(ssrfProtectionService).validateDestinationUrl(anyString(), anyBoolean());
        when(destinationRepository.save(any(DestinationEntity.class))).thenAnswer(i -> i.getArgument(0));

        DestinationUpdateRequest updateRequest = new DestinationUpdateRequest(
                "Alpha FHIR Renamed", "https://fhir.hospital-a.com/r4", "R4",
                DestinationAuthType.BEARER_TOKEN, "", true // Blank token
        );

        DestinationResponse updated = destinationService.updateDestination(10L, updateRequest);

        assertEquals("Alpha FHIR Renamed", updated.getName());
        assertTrue(updated.isHasAuthToken());
        // Verify token was retained
        assertEquals("encrypted-token-123", destinationA.getAuthToken());
    }

    @Test
    @DisplayName("Hospital user cannot modify or delete platform-wide destination")
    void testHospitalUserCannotModifyPlatformDestination() {
        when(destinationRepository.findById(20L)).thenReturn(Optional.of(platformDestination));
        when(tenantSecurityService.isPlatformAdmin()).thenReturn(false);

        DestinationUpdateRequest updateRequest = new DestinationUpdateRequest(
                "Modified Mock", "http://localhost:8080/api/mock-fhir", "R4",
                DestinationAuthType.NONE, null, true
        );

        assertThrows(TenantAccessDeniedException.class,
                () -> destinationService.updateDestination(20L, updateRequest));

        assertThrows(TenantAccessDeniedException.class,
                () -> destinationService.deleteDestination(20L));
    }

    @Test
    @DisplayName("Deleting non-existent destination throws DestinationNotFoundException")
    void testDeleteNonExistentDestination() {
        when(destinationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(DestinationNotFoundException.class, () -> destinationService.deleteDestination(999L));
    }
}
