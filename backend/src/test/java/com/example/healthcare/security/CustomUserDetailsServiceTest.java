package com.example.healthcare.security;

import com.example.healthcare.entity.User;
import com.example.healthcare.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User("admin", "$2a$10$encryptedHashExample", "ADMIN", true);
    }

    @Test
    @DisplayName("Successfully loads existing user by username and prefixes role with ROLE_")
    void shouldLoadUserByUsername() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(sampleUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername("admin");

        assertNotNull(userDetails);
        assertEquals("admin", userDetails.getUsername());
        assertEquals("$2a$10$encryptedHashExample", userDetails.getPassword());
        assertTrue(userDetails.isEnabled());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        verify(userRepository).findByUsername("admin");
    }

    @Test
    @DisplayName("Trims whitespace from input username before repository lookup")
    void shouldTrimUsernameBeforeLookup() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(sampleUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername("  admin  ");

        assertNotNull(userDetails);
        assertEquals("admin", userDetails.getUsername());
        verify(userRepository).findByUsername("admin");
    }

    @Test
    @DisplayName("Throws UsernameNotFoundException when user is not present in repository")
    void shouldThrowWhenUserNotFound() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("nonexistent"));

        verify(userRepository).findByUsername("nonexistent");
    }
}
