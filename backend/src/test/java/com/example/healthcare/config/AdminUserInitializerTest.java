package com.example.healthcare.config;

import com.example.healthcare.entity.User;
import com.example.healthcare.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private Environment environment;

    private AdminUserInitializer initializer;

    @BeforeEach
    void setUp() {
        initializer = new AdminUserInitializer(userRepository, passwordEncoder, environment);
        ReflectionTestUtils.setField(initializer, "seedEnabled", true);
        ReflectionTestUtils.setField(initializer, "adminUsername", "admin");
        ReflectionTestUtils.setField(initializer, "adminPassword", "admin123");
    }

    @Test
    @DisplayName("Non-production: seeds default admin123 when user does not exist")
    void shouldSeedDefaultAdminInNonProdWhenNotExists() {
        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(false);
        when(passwordEncoder.encode("admin123")).thenReturn("encodedAdmin123");

        initializer.run();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertEquals("admin", saved.getUsername());
        assertEquals("encodedAdmin123", saved.getPassword());
        assertEquals("ADMIN", saved.getRole());
        assertTrue(saved.isEnabled());
    }

    @Test
    @DisplayName("Non-production: skips seeding when admin already exists")
    void shouldSkipSeedingWhenUserAlreadyExists() {
        when(userRepository.existsByUsername("admin")).thenReturn(true);

        initializer.run();

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Production: refuses to seed when password is the default public password admin123")
    void shouldRefuseToSeedInProductionWithDefaultPassword() {
        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(true);

        initializer.run();

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Production: seeds admin when secure custom password is provided")
    void shouldSeedInProductionWithCustomSecurePassword() {
        ReflectionTestUtils.setField(initializer, "adminPassword", "SuperSecureProdSecret#2026");
        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(true);
        when(passwordEncoder.encode("SuperSecureProdSecret#2026")).thenReturn("encodedSecurePassword");

        initializer.run();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertEquals("admin", saved.getUsername());
        assertEquals("encodedSecurePassword", saved.getPassword());
    }

    @Test
    @DisplayName("Production: refuses to seed when custom password is shorter than 8 characters")
    void shouldRefuseToSeedInProductionWhenPasswordIsTooShort() {
        ReflectionTestUtils.setField(initializer, "adminPassword", "short");
        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(true);

        initializer.run();

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Trims whitespace from admin username when seeding")
    void shouldTrimUsernameWhenSeeding() {
        ReflectionTestUtils.setField(initializer, "adminUsername", "  admin  ");
        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPass");

        initializer.run();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("admin", userCaptor.getValue().getUsername());
    }

    @Test
    @DisplayName("Disabled seeding: skips seeding even if user does not exist")
    void shouldSkipWhenSeedingIsDisabled() {
        ReflectionTestUtils.setField(initializer, "seedEnabled", false);

        initializer.run();

        verify(userRepository, never()).existsByUsername(anyString());
        verify(userRepository, never()).save(any(User.class));
    }
}
