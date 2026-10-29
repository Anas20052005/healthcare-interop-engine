package com.example.healthcare.config;

import com.example.healthcare.entity.User;
import com.example.healthcare.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Initializes default administrative user idempotently on application startup.
 * Complies with HIPAA/privacy regulations: password is encrypted with BCrypt
 * and never logged or stored as plaintext.
 *
 * In production profiles (prod, production), seeding with the publicly known
 * default password ('admin123') is strictly prohibited.
 */
@Component
public class AdminUserInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminUserInitializer.class);
    private static final String DEFAULT_INSECURE_PASSWORD = "admin123";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    @Value("${app.admin.seed:true}")
    private boolean seedEnabled;

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.password:}")
    private String adminPassword;

    public AdminUserInitializer(UserRepository userRepository,
                                PasswordEncoder passwordEncoder,
                                Environment environment) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
    }

    @Override
    public void run(String... args) {
        if (!seedEnabled) {
            logger.info("Admin user seeding is explicitly disabled via configuration (app.admin.seed=false).");
            return;
        }

        String cleanUsername = (adminUsername != null && !adminUsername.trim().isEmpty()) ? adminUsername.trim() : "admin";

        if (userRepository.existsByUsername(cleanUsername)) {
            logger.debug("Admin user '{}' already exists in the database. Skipping initialization.", cleanUsername);
            return;
        }

        boolean isProduction = environment.acceptsProfiles(Profiles.of("prod", "production"));
        String cleanPassword;

        if (isProduction) {
            String trimmedPassword = (adminPassword != null) ? adminPassword.trim() : "";
            if (trimmedPassword.isEmpty() || DEFAULT_INSECURE_PASSWORD.equals(trimmedPassword)) {
                logger.warn("SECURITY WARNING: Production profile active and no custom ADMIN_PASSWORD provided. "
                        + "Default publicly known credentials ('{}') will NOT be seeded into production. "
                        + "Set the ADMIN_PASSWORD environment variable with a strong password (at least 8 characters) to create the initial admin.",
                        DEFAULT_INSECURE_PASSWORD);
                return;
            }
            if (trimmedPassword.length() < 8) {
                logger.warn("SECURITY WARNING: Configured ADMIN_PASSWORD in production must be at least 8 characters long. Skipping admin user seeding.");
                return;
            }
            cleanPassword = trimmedPassword;
            logger.info("Seeding initial production admin user '{}' with securely configured password.", cleanUsername);
        } else {
            if (adminPassword == null || adminPassword.trim().isEmpty()) {
                logger.warn("No ADMIN_PASSWORD configured in non-production. Defaulting to '{}' for development convenience.",
                        DEFAULT_INSECURE_PASSWORD);
                cleanPassword = DEFAULT_INSECURE_PASSWORD;
            } else {
                cleanPassword = adminPassword.trim();
            }
            logger.info("Seeding initial development/test admin user '{}'.", cleanUsername);
        }

        User admin = new User(cleanUsername, passwordEncoder.encode(cleanPassword), "ADMIN", true);
        userRepository.save(admin);
        logger.info("Admin user '{}' seeded successfully with BCrypt encrypted password.", cleanUsername);
    }
}

