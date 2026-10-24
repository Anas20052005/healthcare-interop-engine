package com.example.healthcare.config;

import com.example.healthcare.entity.DestinationAuthType;
import com.example.healthcare.entity.DestinationEntity;
import com.example.healthcare.repository.DestinationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Initializes the default internal development/testing mock FHIR destination server
 * idempotently on application startup.
 * Gated to non-production environments only.
 */
@Component
@Profile("!prod & !production")
@ConditionalOnProperty(name = "app.seed-mock-destination", havingValue = "true", matchIfMissing = true)
public class DestinationInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DestinationInitializer.class);

    private final DestinationRepository destinationRepository;

    @Value("${server.port:8080}")
    private int serverPort;

    public DestinationInitializer(DestinationRepository destinationRepository) {
        this.destinationRepository = destinationRepository;
    }

    @Override
    public void run(String... args) {
        if (!destinationRepository.existsByMockTrue()) {
            String mockUrl = "http://localhost:" + serverPort + "/api/mock-fhir";
            DestinationEntity mockDestination = new DestinationEntity(
                    "Local Mock FHIR Server (Dev/Test)",
                    mockUrl,
                    "R4",
                    DestinationAuthType.NONE,
                    true,
                    true,
                    null // Platform-wide scope
            );
            destinationRepository.save(mockDestination);
            logger.info("Default internal development mock FHIR destination seeded successfully at {}", mockUrl);
        } else {
            logger.debug("Mock FHIR destination already present. Skipping initialization.");
        }
    }
}
