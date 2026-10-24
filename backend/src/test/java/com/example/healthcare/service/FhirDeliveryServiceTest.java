package com.example.healthcare.service;

import ca.uhn.fhir.context.FhirContext;
import com.example.healthcare.dto.delivery.DeliveryResultResponse;
import com.example.healthcare.entity.ConversionEntity;
import com.example.healthcare.entity.DeliveryResultEntity;
import com.example.healthcare.entity.DestinationAuthType;
import com.example.healthcare.entity.DestinationEntity;
import com.example.healthcare.entity.Hospital;
import com.example.healthcare.exception.DestinationNotFoundException;
import com.example.healthcare.exception.InvalidDestinationException;
import com.example.healthcare.exception.ResourceNotFoundException;
import com.example.healthcare.exception.TenantAccessDeniedException;
import com.example.healthcare.repository.ConversionRepository;
import com.example.healthcare.repository.DeliveryResultRepository;
import com.example.healthcare.repository.DestinationRepository;
import com.example.healthcare.security.TenantSecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
class FhirDeliveryServiceTest {

    @Mock
    private ConversionRepository conversionRepository;

    @Mock
    private DestinationRepository destinationRepository;

    @Mock
    private DeliveryResultRepository deliveryResultRepository;

    @Mock
    private TenantSecurityService tenantSecurityService;

    @Mock
    private SsrfProtectionService ssrfProtectionService;

    @Mock
    private TokenCryptoService tokenCryptoService;

    @Mock
    private MockFhirService mockFhirService;

    @Mock
    private RestTemplate restTemplate;

    private FhirContext fhirContext;
    private DefaultFhirDeliveryService deliveryService;

    private Hospital hospitalA;
    private Hospital hospitalB;
    private ConversionEntity conversionA;
    private DestinationEntity destinationA;

    private static final String VALID_FHIR_BUNDLE = """
            {
              "resourceType": "Bundle",
              "type": "collection",
              "entry": [
                {
                  "resource": {
                    "resourceType": "Patient",
                    "id": "pat-1"
                  }
                }
              ]
            }
            """;

    @BeforeEach
    void setUp() {
        fhirContext = FhirContext.forR4();
        deliveryService = new DefaultFhirDeliveryService(
                conversionRepository,
                destinationRepository,
                deliveryResultRepository,
                tenantSecurityService,
                ssrfProtectionService,
                tokenCryptoService,
                mockFhirService,
                fhirContext,
                restTemplate
        );

        hospitalA = new Hospital("Hospital Alpha", "HOSP_A");
        hospitalA.setId(1L);

        hospitalB = new Hospital("Hospital Beta", "HOSP_B");
        hospitalB.setId(2L);

        conversionA = new ConversionEntity("CTRL001", "ADT^A01", "SUCCESS", "P001", null, hospitalA);
        conversionA.setId(100L);
        conversionA.setFhirPayload(VALID_FHIR_BUNDLE);

        destinationA = new DestinationEntity("Test Destination", "https://fhir.example.org/r4", "R4",
                DestinationAuthType.BEARER_TOKEN, true, false, hospitalA);
        destinationA.setId(200L);
        destinationA.setAuthToken("encrypted-token-xyz");
    }

    @Test
    @DisplayName("Send conversion to destination succeeds and persists SUCCESS delivery result")
    void testSendConversionSuccess() {
        when(conversionRepository.findById(100L)).thenReturn(Optional.of(conversionA));
        when(destinationRepository.findById(200L)).thenReturn(Optional.of(destinationA));
        doNothing().when(tenantSecurityService).validateResourceHospital(hospitalA);
        doNothing().when(ssrfProtectionService).validateDestinationUrl(anyString(), anyBoolean());
        when(tokenCryptoService.decrypt("encrypted-token-xyz")).thenReturn("plain-token-123");

        ResponseEntity<String> mockResponse = new ResponseEntity<>("{\"resourceType\": \"OperationOutcome\"}", HttpStatus.CREATED);
        when(restTemplate.exchange(eq("https://fhir.example.org/r4"), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(mockResponse);

        when(deliveryResultRepository.save(any(DeliveryResultEntity.class))).thenAnswer(i -> {
            DeliveryResultEntity entity = i.getArgument(0);
            entity.setId(500L);
            return entity;
        });

        DeliveryResultResponse response = deliveryService.sendConversion(100L, 200L);

        assertNotNull(response);
        assertEquals(500L, response.getId());
        assertEquals(100L, response.getConversionId());
        assertEquals(200L, response.getDestinationId());
        assertEquals("SUCCESS", response.getStatus());
        assertEquals(201, response.getHttpStatusCode());
        assertNull(response.getErrorMessage());

        // Verify Bearer token header was passed
        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(eq("https://fhir.example.org/r4"), eq(HttpMethod.POST), entityCaptor.capture(), eq(String.class));
        HttpEntity captured = entityCaptor.getValue();
        assertEquals("Bearer plain-token-123", captured.getHeaders().getFirst("Authorization"));
    }

    @Test
    @DisplayName("Send conversion to destination handles HTTP 400 error and records FAILED delivery result")
    void testSendConversionHttpError() {
        when(conversionRepository.findById(100L)).thenReturn(Optional.of(conversionA));
        when(destinationRepository.findById(200L)).thenReturn(Optional.of(destinationA));
        doNothing().when(tenantSecurityService).validateResourceHospital(hospitalA);
        doNothing().when(ssrfProtectionService).validateDestinationUrl(anyString(), anyBoolean());
        when(tokenCryptoService.decrypt("encrypted-token-xyz")).thenReturn("plain-token-123");

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad Request"));

        when(deliveryResultRepository.save(any(DeliveryResultEntity.class))).thenAnswer(i -> {
            DeliveryResultEntity entity = i.getArgument(0);
            entity.setId(501L);
            return entity;
        });

        DeliveryResultResponse response = deliveryService.sendConversion(100L, 200L);

        assertNotNull(response);
        assertEquals("FAILED", response.getStatus());
        assertEquals(400, response.getHttpStatusCode());
        assertTrue(response.getErrorMessage().contains("400"));
    }

    @Test
    @DisplayName("Send conversion handles connection timeout gracefully and records FAILED status with 504")
    void testSendConversionTimeout() {
        when(conversionRepository.findById(100L)).thenReturn(Optional.of(conversionA));
        when(destinationRepository.findById(200L)).thenReturn(Optional.of(destinationA));
        doNothing().when(tenantSecurityService).validateResourceHospital(hospitalA);
        doNothing().when(ssrfProtectionService).validateDestinationUrl(anyString(), anyBoolean());
        when(tokenCryptoService.decrypt("encrypted-token-xyz")).thenReturn("plain-token-123");

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new ResourceAccessException("Read timed out", new SocketTimeoutException("Read timed out")));

        when(deliveryResultRepository.save(any(DeliveryResultEntity.class))).thenAnswer(i -> {
            DeliveryResultEntity entity = i.getArgument(0);
            entity.setId(502L);
            return entity;
        });

        DeliveryResultResponse response = deliveryService.sendConversion(100L, 200L);

        assertNotNull(response);
        assertEquals("RETRYING", response.getStatus());
        assertEquals(504, response.getHttpStatusCode());
        assertNotNull(response.getNextRetryAt());
        assertTrue(response.getErrorMessage().contains("timed out"));
    }

    @Test
    @DisplayName("Send conversion rejects sending when destination is disabled")
    void testSendConversionDisabledDestination() {
        destinationA.setEnabled(false);

        when(conversionRepository.findById(100L)).thenReturn(Optional.of(conversionA));
        when(destinationRepository.findById(200L)).thenReturn(Optional.of(destinationA));
        doNothing().when(tenantSecurityService).validateResourceHospital(hospitalA);

        assertThrows(InvalidDestinationException.class, () -> deliveryService.sendConversion(100L, 200L));
    }

    @Test
    @DisplayName("Send conversion fails if stored payload is invalid FHIR and records FAILED delivery result")
    void testSendConversionInvalidPayload() {
        conversionA.setFhirPayload("NOT_VALID_FHIR_JSON");

        when(conversionRepository.findById(100L)).thenReturn(Optional.of(conversionA));
        when(destinationRepository.findById(200L)).thenReturn(Optional.of(destinationA));
        doNothing().when(tenantSecurityService).validateResourceHospital(hospitalA);
        doNothing().when(ssrfProtectionService).validateDestinationUrl(anyString(), anyBoolean());

        when(deliveryResultRepository.save(any(DeliveryResultEntity.class))).thenAnswer(i -> {
            DeliveryResultEntity entity = i.getArgument(0);
            entity.setId(503L);
            return entity;
        });

        DeliveryResultResponse response = deliveryService.sendConversion(100L, 200L);

        assertEquals("FAILED", response.getStatus());
        assertNull(response.getHttpStatusCode());
        assertTrue(response.getErrorMessage().contains("Payload validation failed"));
    }

    @Test
    @DisplayName("Hospital A user cannot send Hospital B's conversion (cross-hospital security violation)")
    void testSendCrossHospitalConversionDenied() {
        ConversionEntity conversionB = new ConversionEntity("CTRL002", "ADT^A01", "SUCCESS", "P002", null, hospitalB);
        conversionB.setId(101L);

        when(conversionRepository.findById(101L)).thenReturn(Optional.of(conversionB));
        doThrow(new TenantAccessDeniedException("Cross tenant violation"))
                .when(tenantSecurityService).validateResourceHospital(hospitalB);

        assertThrows(TenantAccessDeniedException.class, () -> deliveryService.sendConversion(101L, 200L));
    }

    @Test
    @DisplayName("Send conversion to embedded Mock FHIR server succeeds and persists SUCCESS with response summary")
    void testSendConversionToMockServerSuccess() {
        DestinationEntity mockDest = new DestinationEntity("Mock Destination", "http://localhost:8080/api/mock-fhir", "R4",
                DestinationAuthType.NONE, true, true, null);
        mockDest.setId(300L);

        when(conversionRepository.findById(100L)).thenReturn(Optional.of(conversionA));
        when(destinationRepository.findById(300L)).thenReturn(Optional.of(mockDest));
        doNothing().when(tenantSecurityService).validateResourceHospital(hospitalA);
        doNothing().when(ssrfProtectionService).validateDestinationUrl(anyString(), anyBoolean());

        com.example.healthcare.dto.mockfhir.MockReceivedResource mockRecord =
                new com.example.healthcare.dto.mockfhir.MockReceivedResource("track-99", "Bundle", "pat-1", 1, "Bundle summary", java.time.LocalDateTime.now(), "{}", false);
        when(mockFhirService.processReceivedPayload(anyString(), any(), anyString())).thenReturn(mockRecord);

        when(deliveryResultRepository.save(any(DeliveryResultEntity.class))).thenAnswer(i -> {
            DeliveryResultEntity entity = i.getArgument(0);
            entity.setId(504L);
            return entity;
        });

        DeliveryResultResponse response = deliveryService.sendConversion(100L, 300L);

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertEquals(201, response.getHttpStatusCode());
        assertNotNull(response.getResponseSummary());
        assertTrue(response.getResponseSummary().contains("Mock FHIR server"));
    }

    @Test
    @DisplayName("Send conversion throws ResourceNotFoundException for non-existent conversion ID")
    void testSendConversionInvalidConversionId() {
        when(conversionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> deliveryService.sendConversion(999L, 200L));
    }

    @Test
    @DisplayName("Send conversion throws DestinationNotFoundException for non-existent destination ID")
    void testSendConversionInvalidDestinationId() {
        when(conversionRepository.findById(100L)).thenReturn(Optional.of(conversionA));
        doNothing().when(tenantSecurityService).validateResourceHospital(hospitalA);
        when(destinationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(DestinationNotFoundException.class, () -> deliveryService.sendConversion(100L, 999L));
    }

    @Test
    @DisplayName("getDeliveryById returns delivery result response for authorized hospital")
    void testGetDeliveryByIdSuccess() {
        DeliveryResultEntity entity = new DeliveryResultEntity(100L, 200L, "Dest", java.time.LocalDateTime.now(),
                "SUCCESS", 200, null, "Server accepted resource", hospitalA);
        entity.setId(777L);

        when(deliveryResultRepository.findById(777L)).thenReturn(Optional.of(entity));
        doNothing().when(tenantSecurityService).validateResourceHospital(hospitalA);

        DeliveryResultResponse response = deliveryService.getDeliveryById(777L);

        assertNotNull(response);
        assertEquals(777L, response.getId());
        assertEquals("SUCCESS", response.getStatus());
        assertEquals(200, response.getHttpStatusCode());
        assertEquals("Server accepted resource", response.getResponseSummary());
    }

    @Test
    @DisplayName("getDeliveryById denies access for cross-hospital delivery record")
    void testGetDeliveryByIdCrossHospitalDenied() {
        DeliveryResultEntity entity = new DeliveryResultEntity(100L, 200L, "Dest", java.time.LocalDateTime.now(),
                "SUCCESS", 200, null, null, hospitalB);
        entity.setId(888L);

        when(deliveryResultRepository.findById(888L)).thenReturn(Optional.of(entity));
        doThrow(new TenantAccessDeniedException("Cross tenant violation"))
                .when(tenantSecurityService).validateResourceHospital(hospitalB);

        assertThrows(TenantAccessDeniedException.class, () -> deliveryService.getDeliveryById(888L));
    }

    @Test
    @DisplayName("getDeliveryById throws ResourceNotFoundException when delivery record does not exist")
    void testGetDeliveryByIdNotFound() {
        when(deliveryResultRepository.findById(9999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> deliveryService.getDeliveryById(9999L));
    }
}
