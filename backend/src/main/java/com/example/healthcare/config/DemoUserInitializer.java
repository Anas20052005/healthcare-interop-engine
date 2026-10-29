package com.example.healthcare.config;

import com.example.healthcare.entity.Hospital;
import com.example.healthcare.entity.User;
import com.example.healthcare.repository.HospitalRepository;
import com.example.healthcare.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Initializes a dedicated sandbox demo recruiter account idempotently on application startup.
 * Complies with HIPAA/privacy regulations:
 * - Dedicated secret configured via backend environment variable (DEMO_PASSWORD).
 * - Passwords are never hardcoded, never logged, and hashed with BCrypt.
 * - Granted standard 'USER' role only - never ROLE_ADMIN.
 * - Associated with a dedicated demo hospital to preserve strict multi-tenant isolation.
 * - If DEMO_PASSWORD is empty or unset, seeding is safely skipped.
 * - Does not overwrite existing user on restarts.
 */
@Component
public class DemoUserInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DemoUserInitializer.class);

    private final UserRepository userRepository;
    private final HospitalRepository hospitalRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.demo.seed:true}")
    private boolean seedEnabled;

    @Value("${app.demo.username:demo_recruiter}")
    private String demoUsername;

    @Value("${app.demo.password:}")
    private String demoPassword;

    @Value("${app.demo.hospital-name:Recruiter Demo Hospital}")
    private String demoHospitalName;

    @Value("${app.demo.hospital-code:DEMO-REC}")
    private String demoHospitalCode;

    public DemoUserInitializer(UserRepository userRepository,
                               HospitalRepository hospitalRepository,
                               PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.hospitalRepository = hospitalRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!seedEnabled) {
            logger.info("Demo user seeding is explicitly disabled via configuration (app.demo.seed=false).");
            return;
        }

        String cleanUsername = (demoUsername != null && !demoUsername.trim().isEmpty())
                ? demoUsername.trim()
                : "demo_recruiter";

        String cleanPassword = (demoPassword != null) ? demoPassword.trim() : "";

        // If DEMO_PASSWORD is not provided or empty, safely skip initialization
        if (cleanPassword.isEmpty()) {
            logger.info("Demo user initialization skipped: DEMO_PASSWORD is not set or is empty.");
            return;
        }

        if (cleanPassword.length() < 8) {
            logger.warn("SECURITY WARNING: Configured DEMO_PASSWORD must be at least 8 characters long. Skipping demo user seeding.");
            return;
        }

        // Idempotent check: never overwrite an existing user's password on restart
        if (userRepository.existsByUsername(cleanUsername)) {
            logger.debug("Demo user '{}' already exists in the database. Skipping initialization.", cleanUsername);
            return;
        }

        // Ensure dedicated demo hospital exists for multi-tenant sandbox isolation
        String cleanHospitalCode = (demoHospitalCode != null && !demoHospitalCode.trim().isEmpty())
                ? demoHospitalCode.trim()
                : "DEMO-REC";

        String cleanHospitalName = (demoHospitalName != null && !demoHospitalName.trim().isEmpty())
                ? demoHospitalName.trim()
                : "Recruiter Demo Hospital";

        Hospital demoHospital = hospitalRepository.findByCode(cleanHospitalCode).orElseGet(() -> {
            Hospital newHospital = new Hospital(
                    cleanHospitalName,
                    cleanHospitalCode,
                    "demo@interop-engine.example.com",
                    "555-0199",
                    "100 Innovation Way, Suite 400",
                    true
            );
            Hospital saved = hospitalRepository.save(newHospital);
            logger.info("Created dedicated sandbox demo hospital '{}' ({})", saved.getName(), saved.getCode());
            return saved;
        });

        // Seed demo account with standard USER role (never ADMIN)
        User demoUser = new User(
                cleanUsername,
                passwordEncoder.encode(cleanPassword),
                "USER",
                demoHospital,
                true
        );
        userRepository.save(demoUser);

        logger.info("Demo recruiter user '{}' seeded successfully with role 'USER' associated with hospital '{}' ({})",
                cleanUsername, demoHospital.getName(), demoHospital.getCode());
    }
}

