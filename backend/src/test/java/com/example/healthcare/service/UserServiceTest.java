package com.example.healthcare.service;

import com.example.healthcare.dto.user.CreateUserRequest;
import com.example.healthcare.dto.user.CreateUserResponse;
import com.example.healthcare.dto.user.UpdateUserRequest;
import com.example.healthcare.dto.user.UserResponse;
import com.example.healthcare.entity.Hospital;
import com.example.healthcare.entity.User;
import com.example.healthcare.exception.HospitalNotFoundException;
import com.example.healthcare.exception.InvalidHospitalException;
import com.example.healthcare.exception.TenantAccessDeniedException;
import com.example.healthcare.exception.UserAlreadyExistsException;
import com.example.healthcare.repository.HospitalRepository;
import com.example.healthcare.repository.UserRepository;
import com.example.healthcare.security.TenantSecurityService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private HospitalRepository hospitalRepository;

    @Mock
    private TenantSecurityService tenantSecurityService;

    private DefaultUserService userService;

    @BeforeEach
    void setUp() {
        userService = new DefaultUserService(userRepository, passwordEncoder, hospitalRepository, tenantSecurityService);
    }

    @Test
    @DisplayName("createUser without hospital should create platform-level user")
    void shouldCreateUserSuccessfullyWithoutHospital() {
        CreateUserRequest request = new CreateUserRequest("newdoctor", "SecurePassword123", "ROLE_USER");

        when(userRepository.existsByUsername("newdoctor")).thenReturn(false);
        when(passwordEncoder.encode("SecurePassword123")).thenReturn("$2a$10$encodedHashString");

        User savedUser = new User("newdoctor", "$2a$10$encodedHashString", "USER", true);
        savedUser.setId(10L);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        CreateUserResponse response = userService.createUser(request);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("newdoctor", response.getUsername());
        assertEquals("USER", response.getRole());
        assertTrue(response.isEnabled());
        assertNull(response.getHospitalId());
        assertNull(response.getHospitalName());
        assertEquals("User created successfully", response.getMessage());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User persisted = captor.getValue();
        assertEquals("newdoctor", persisted.getUsername());
        assertEquals("$2a$10$encodedHashString", persisted.getPassword());
        assertEquals("USER", persisted.getRole());
        assertTrue(persisted.isEnabled());
        assertNull(persisted.getHospital());
    }

    @Test
    @DisplayName("createUser with valid enabled hospital should associate hospital")
    void shouldCreateUserWithValidHospital() {
        CreateUserRequest request = new CreateUserRequest("hospitaluser", "SecurePassword123", "ADMIN", 1L);

        Hospital hospital = new Hospital("General Hospital", "GEN_HOSP", "info@genhosp.org", "555-0100", "123 Main St", true);
        hospital.setId(1L);

        when(userRepository.existsByUsername("hospitaluser")).thenReturn(false);
        when(hospitalRepository.findById(1L)).thenReturn(Optional.of(hospital));
        when(passwordEncoder.encode("SecurePassword123")).thenReturn("$2a$10$encodedHashString");

        User savedUser = new User("hospitaluser", "$2a$10$encodedHashString", "ADMIN", hospital, true);
        savedUser.setId(11L);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        CreateUserResponse response = userService.createUser(request);

        assertNotNull(response);
        assertEquals(11L, response.getId());
        assertEquals("hospitaluser", response.getUsername());
        assertEquals("ADMIN", response.getRole());
        assertEquals(1L, response.getHospitalId());
        assertEquals("General Hospital", response.getHospitalName());
        assertEquals("GEN_HOSP", response.getHospitalCode());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User persisted = captor.getValue();
        assertEquals("hospitaluser", persisted.getUsername());
        assertNotNull(persisted.getHospital());
        assertEquals("GEN_HOSP", persisted.getHospital().getCode());
    }

    @Test
    @DisplayName("Hospital admin cannot create user for another hospital")
    void shouldRejectHospitalAdminCreatingUserForAnotherHospital() {
        CreateUserRequest request = new CreateUserRequest("intruder", "SecurePassword123", "USER", 2L);

        Hospital hospitalA = new Hospital("Hospital A", "HOSP_A");
        hospitalA.setId(1L);

        User callerAdminA = new User("adminA", "hash", "ADMIN", hospitalA, true);

        when(userRepository.existsByUsername("intruder")).thenReturn(false);
        when(tenantSecurityService.getCurrentUser()).thenReturn(Optional.of(callerAdminA));
        when(tenantSecurityService.isPlatformAdmin()).thenReturn(false);
        when(tenantSecurityService.isHospitalAdmin()).thenReturn(true);
        when(tenantSecurityService.requireCurrentHospital()).thenReturn(hospitalA);

        assertThrows(TenantAccessDeniedException.class, () -> userService.createUser(request));
    }

    @Test
    @DisplayName("Hospital admin cannot create platform administrator")
    void shouldRejectHospitalAdminCreatingPlatformAdmin() {
        CreateUserRequest request = new CreateUserRequest("superadmin2", "SecurePassword123", "ADMIN", null);

        Hospital hospitalA = new Hospital("Hospital A", "HOSP_A");
        hospitalA.setId(1L);

        User callerAdminA = new User("adminA", "hash", "ADMIN", hospitalA, true);

        when(userRepository.existsByUsername("superadmin2")).thenReturn(false);
        when(tenantSecurityService.getCurrentUser()).thenReturn(Optional.of(callerAdminA));
        when(tenantSecurityService.isPlatformAdmin()).thenReturn(false);
        when(tenantSecurityService.isHospitalAdmin()).thenReturn(true);
        when(tenantSecurityService.requireCurrentHospital()).thenReturn(hospitalA);

        assertThrows(TenantAccessDeniedException.class, () -> userService.createUser(request));
    }

    @Test
    @DisplayName("Hospital admin only gets users for their own hospital")
    void shouldScopeUserListToHospital() {
        Hospital hospitalA = new Hospital("Hospital A", "HOSP_A");
        hospitalA.setId(1L);

        User u1 = new User("userA1", "hash", "USER", hospitalA, true);
        u1.setId(101L);

        when(tenantSecurityService.isPlatformAdmin()).thenReturn(false);
        when(tenantSecurityService.requireCurrentHospital()).thenReturn(hospitalA);
        when(userRepository.findAllByHospitalId(1L)).thenReturn(List.of(u1));

        List<UserResponse> users = userService.getAllUsers();
        assertEquals(1, users.size());
        assertEquals("userA1", users.get(0).getUsername());
    }

    @Test
    @DisplayName("createUser with nonexistent hospital should throw HospitalNotFoundException")
    void shouldThrowWhenHospitalNotFound() {
        CreateUserRequest request = new CreateUserRequest("hospitaluser", "SecurePassword123", "ADMIN", 999L);

        when(userRepository.existsByUsername("hospitaluser")).thenReturn(false);
        when(hospitalRepository.findById(999L)).thenReturn(Optional.empty());

        HospitalNotFoundException ex = assertThrows(
                HospitalNotFoundException.class,
                () -> userService.createUser(request)
        );

        assertTrue(ex.getMessage().contains("999"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("createUser with disabled hospital should throw InvalidHospitalException")
    void shouldThrowWhenHospitalIsDisabled() {
        CreateUserRequest request = new CreateUserRequest("hospitaluser", "SecurePassword123", "ADMIN", 2L);

        Hospital disabledHospital = new Hospital("Closed Clinic", "CLOSED_01", null, null, null, false);
        disabledHospital.setId(2L);

        when(userRepository.existsByUsername("hospitaluser")).thenReturn(false);
        when(hospitalRepository.findById(2L)).thenReturn(Optional.of(disabledHospital));

        InvalidHospitalException ex = assertThrows(
                InvalidHospitalException.class,
                () -> userService.createUser(request)
        );

        assertTrue(ex.getMessage().contains("disabled hospital"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("createUser should throw UserAlreadyExistsException when username already exists")
    void shouldThrowWhenUsernameAlreadyExists() {
        CreateUserRequest request = new CreateUserRequest("existingadmin", "SecurePassword123", "ADMIN");
        when(userRepository.existsByUsername("existingadmin")).thenReturn(true);

        UserAlreadyExistsException ex = assertThrows(
                UserAlreadyExistsException.class,
                () -> userService.createUser(request)
        );

        assertTrue(ex.getMessage().contains("existingadmin"));
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("updateUser by platform admin can assign hospital and update role")
    void shouldUpdateUserByPlatformAdmin() {
        Hospital hospital = new Hospital("General Hospital", "GH_01", null, null, null, true);
        hospital.setId(1L);

        User existingUser = new User("doctor_a", "hashedpwd", "USER", true);
        existingUser.setId(10L);

        when(userRepository.findById(10L)).thenReturn(Optional.of(existingUser));
        when(tenantSecurityService.isPlatformAdmin()).thenReturn(true);
        when(hospitalRepository.findById(1L)).thenReturn(Optional.of(hospital));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateUserRequest updateRequest = new UpdateUserRequest("ADMIN", 1L, true);
        UserResponse response = userService.updateUser(10L, updateRequest);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("ADMIN", response.getRole());
        assertEquals(1L, response.getHospitalId());
        assertEquals("General Hospital", response.getHospitalName());
    }

    @Test
    @DisplayName("updateUser by hospital admin cannot reassign user to different hospital")
    void shouldRejectHospitalAdminReassigningUser() {
        Hospital hospitalA = new Hospital("Hospital A", "HA_01", null, null, null, true);
        hospitalA.setId(1L);

        User existingUser = new User("staff_a", "hashedpwd", "USER", true);
        existingUser.setId(15L);
        existingUser.setHospital(hospitalA);

        when(userRepository.findById(15L)).thenReturn(Optional.of(existingUser));
        when(tenantSecurityService.isPlatformAdmin()).thenReturn(false);
        when(tenantSecurityService.isHospitalAdmin()).thenReturn(true);
        when(tenantSecurityService.requireCurrentHospital()).thenReturn(hospitalA);

        UpdateUserRequest updateRequest = new UpdateUserRequest("USER", 2L, true);
        assertThrows(TenantAccessDeniedException.class, () -> userService.updateUser(15L, updateRequest));
    }

    @Test
    @DisplayName("updateUser assigns active hospital to clinical user with null hospital and preserves password hash")
    void shouldAssignHospitalToClinicalUserWithNullHospital() {
        Hospital hospital = new Hospital("Metro Health", "MH_01", null, null, null, true);
        hospital.setId(5L);

        User clinicalUserNoHospital = new User("clinical_null", "$2a$10$existingHashedPassword123", "USER", true);
        clinicalUserNoHospital.setId(20L);
        assertNull(clinicalUserNoHospital.getHospital());

        when(userRepository.findById(20L)).thenReturn(Optional.of(clinicalUserNoHospital));
        when(tenantSecurityService.isPlatformAdmin()).thenReturn(true);
        when(hospitalRepository.findById(5L)).thenReturn(Optional.of(hospital));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateUserRequest updateRequest = new UpdateUserRequest("USER", 5L, true);
        UserResponse response = userService.updateUser(20L, updateRequest);

        assertNotNull(response);
        assertEquals(20L, response.getId());
        assertEquals("clinical_null", response.getUsername());
        assertEquals("USER", response.getRole());
        assertEquals(5L, response.getHospitalId());
        assertEquals("Metro Health", response.getHospitalName());
        // Verify password was NOT changed or re-encoded
        assertEquals("$2a$10$existingHashedPassword123", clinicalUserNoHospital.getPassword());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("updateUser by platform admin can dissociate hospital when clearHospital is true")
    void shouldDissociateHospitalWhenClearHospitalIsTrue() {
        Hospital hospital = new Hospital("Metro Health", "MH_01", null, null, null, true);
        hospital.setId(5L);

        User userWithHospital = new User("staff_b", "hashedpwd", "USER", true);
        userWithHospital.setId(21L);
        userWithHospital.setHospital(hospital);

        when(userRepository.findById(21L)).thenReturn(Optional.of(userWithHospital));
        when(tenantSecurityService.isPlatformAdmin()).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateUserRequest updateRequest = new UpdateUserRequest("USER", null, true, true);
        UserResponse response = userService.updateUser(21L, updateRequest);

        assertNotNull(response);
        assertEquals(21L, response.getId());
        assertNull(response.getHospitalId());
        assertNull(response.getHospitalName());
        assertNull(userWithHospital.getHospital());
    }

    @Test
    @DisplayName("updateUser by hospital admin cannot dissociate user from hospital")
    void shouldRejectHospitalAdminDissociatingHospital() {
        Hospital hospital = new Hospital("Metro Health", "MH_01", null, null, null, true);
        hospital.setId(5L);

        User userWithHospital = new User("staff_b", "hashedpwd", "USER", true);
        userWithHospital.setId(22L);
        userWithHospital.setHospital(hospital);

        when(userRepository.findById(22L)).thenReturn(Optional.of(userWithHospital));
        when(tenantSecurityService.isPlatformAdmin()).thenReturn(false);
        when(tenantSecurityService.isHospitalAdmin()).thenReturn(true);
        when(tenantSecurityService.requireCurrentHospital()).thenReturn(hospital);

        UpdateUserRequest updateRequest = new UpdateUserRequest("USER", null, true, true);
        assertThrows(TenantAccessDeniedException.class, () -> userService.updateUser(22L, updateRequest));
    }
}
