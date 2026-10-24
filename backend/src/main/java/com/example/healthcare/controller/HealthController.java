package com.example.healthcare.controller;

import com.example.healthcare.dto.common.HealthResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;

@RestController
@RequestMapping("/api")
public class HealthController {

    private static final Logger logger = LoggerFactory.getLogger(HealthController.class);

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> getHealth() {
        boolean dbOk = false;
        try (Connection connection = dataSource.getConnection()) {
            dbOk = connection.isValid(2);
        } catch (Exception ex) {
            logger.warn("Health check failed database connectivity probe: {}", ex.getMessage());
        }

        String dbStatus = dbOk ? "UP" : "DOWN";
        String overallStatus = dbOk ? "UP" : "DOWN";
        String engineStatus = dbOk ? "UP" : "DEGRADED";

        HealthResponse response = new HealthResponse(overallStatus, dbStatus, engineStatus);

        if (!dbOk) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
        return ResponseEntity.ok(response);
    }
}
