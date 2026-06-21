package com.familyexpense.tracker.common;

import jakarta.persistence.EntityManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private final EntityManager entityManager;

    public HealthController(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @GetMapping("/api/health")
    public ResponseEntity<String> health() {
        try {
            entityManager.createNativeQuery("SELECT 1").getSingleResult();
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            return ResponseEntity.status(503).body("Database connection failed: " + e.getMessage());
        }
    }
}
