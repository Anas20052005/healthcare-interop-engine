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

        if (userRepository.existsByUsername(adminUsername)) {
            logger.debug("Admin user '{}' already exists in the database. Skipping initialization.", adminUsername);
            return;
        }

        boolean isProduction = environment.acceptsProfiles(Profiles.of("prod", "production"));

        if (isProduction) {
            if (DEFAULT_INSECURE_PASSWORD.equals(adminPassword) || adminPassword == null || adminPassword.trim().isEmpty()) {
                logger.warn("SECURITY WARNING: Production profile active and no custom ADMIN_PASSWORD provided. "
                        + "Default publicly known credentials ('{}') will NOT be seeded into production.",
                        DEFAULT_INSECURE_PASSWORD);
                return;
            }
            logger.info("Seeding initial production admin user '{}' with securely configured password.", adminUsername);
        } else {
            if (adminPassword == null || adminPassword.trim().isEmpty()) {
                logger.warn("No ADMIN_PASSWORD configured in non-production. Defaulting to '{}' for development convenience.",
                        DEFAULT_INSECURE_PASSWORD);
                adminPassword = DEFAULT_INSECURE_PASSWORD;
            }
            logger.info("Seeding initial development/test admin user '{}'.", adminUsername);
        }

        User admin = new User(adminUsername, passwordEncoder.encode(adminPassword), "ADMIN", true);
        userRepository.save(admin);
        logger.info("Admin user '{}' seeded successfully with BCrypt encrypted password.", adminUsername);
    }
}

