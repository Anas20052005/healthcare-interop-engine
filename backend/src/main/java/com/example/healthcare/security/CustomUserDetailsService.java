package com.example.healthcare.security;

import com.example.healthcare.entity.User;
import com.example.healthcare.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Spring Security UserDetailsService backed by the MySQL users table.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String cleanUsername = (username != null) ? username.trim() : "";
        User user = userRepository.findByUsername(cleanUsername)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + cleanUsername));

        String role = user.getRole();
        String authority = (role != null && role.startsWith("ROLE_")) ? role : "ROLE_" + (role != null ? role : "USER");

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.isEnabled(),
                true,
                true,
                true,
                Collections.singletonList(new SimpleGrantedAuthority(authority))
        );
    }
}
