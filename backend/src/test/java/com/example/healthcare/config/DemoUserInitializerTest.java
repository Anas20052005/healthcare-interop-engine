package com.example.healthcare.config;

import com.example.healthcare.entity.Hospital;
import com.example.healthcare.entity.User;
import com.example.healthcare.repository.HospitalRepository;
import com.example.healthcare.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DemoUserInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private HospitalRepository hospitalRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DemoUserInitializer initializer;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(initializer, "seedEnabled", true);
        ReflectionTestUtils.setField(initializer, "demoUsername", "demo_recruiter");
        ReflectionTestUtils.setField(initializer, "demoPassword", "secureDemoPass123");
        ReflectionTestUtils.setField(initializer, "demoHospitalName", "Recruiter Demo Hospital");
        ReflectionTestUtils.setField(initializer, "demoHospitalCode", "DEMO-REC");
    }

    @Test
    @DisplayName("Safely skips seeding when DEMO_PASSWORD is null or empty")
    void shouldSkipWhenDemoPasswordIsEmpty() {
        ReflectionTestUtils.setField(initializer, "demoPassword", "");

        initializer.run();

        verify(userRepository, never()).save(any(User.class));
        verify(hospitalRepository, never()).save(any(Hospital.class));
    }

    @Test
    @DisplayName("Safely skips seeding when DEMO_PASSWORD is shorter than 8 characters")
    void shouldSkipWhenDemoPasswordIsTooShort() {
        ReflectionTestUtils.setField(initializer, "demoPassword", "short");

        initializer.run();

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Skips seeding when seedEnabled is false")
    void shouldSkipWhenSeedDisabled() {
        ReflectionTestUtils.setField(initializer, "seedEnabled", false);

        initializer.run();

        verify(userRepository, never()).existsByUsername(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Idempotency: does not overwrite existing demo user on restarts")
    void shouldNotOverwriteExistingUser() {
        when(userRepository.existsByUsername("demo_recruiter")).thenReturn(true);

        initializer.run();

        verify(userRepository).existsByUsername("demo_recruiter");
        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("Creates dedicated demo hospital and seeds demo user with role USER (not ADMIN)")
    void shouldCreateDemoHospitalAndSeedUserWhenNotExists() {
        when(userRepository.existsByUsername("demo_recruiter")).thenReturn(false);
        when(hospitalRepository.findByCode("DEMO-REC")).thenReturn(Optional.empty());

        Hospital newHospital = new Hospital("Recruiter Demo Hospital", "DEMO-REC");
        newHospital.setId(99L);
        when(hospitalRepository.save(any(Hospital.class))).thenReturn(newHospital);

        when(passwordEncoder.encode("secureDemoPass123")).thenReturn("$2a$10$hashedDemoPass");

        initializer.run();

        // Verify hospital creation
        ArgumentCaptor<Hospital> hospitalCaptor = ArgumentCaptor.forClass(Hospital.class);
        verify(hospitalRepository).save(hospitalCaptor.capture());
        Hospital capturedHospital = hospitalCaptor.getValue();
        assertEquals("DEMO-REC", capturedHospital.getCode());
        assertEquals("Recruiter Demo Hospital", capturedHospital.getName());
        assertTrue(capturedHospital.isEnabled());

        // Verify user creation
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals("demo_recruiter", savedUser.getUsername());
        assertEquals("$2a$10$hashedDemoPass", savedUser.getPassword());
        assertEquals("USER", savedUser.getRole());
        assertNotEquals("ADMIN", savedUser.getRole(), "Demo user must NEVER be granted ADMIN role");
        assertTrue(savedUser.isEnabled());
        assertNotNull(savedUser.getHospital());
        assertEquals(99L, savedUser.getHospital().getId());
    }

    @Test
    @DisplayName("Reuses existing demo hospital if already present")
    void shouldReuseExistingDemoHospital() {
        when(userRepository.existsByUsername("demo_recruiter")).thenReturn(false);

        Hospital existingHospital = new Hospital("Recruiter Demo Hospital", "DEMO-REC");
        existingHospital.setId(42L);
        when(hospitalRepository.findByCode("DEMO-REC")).thenReturn(Optional.of(existingHospital));

        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");

        initializer.run();

        verify(hospitalRepository, never()).save(any(Hospital.class));
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals(42L, userCaptor.getValue().getHospital().getId());
    }
}

