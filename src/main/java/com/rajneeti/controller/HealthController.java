package com.rajneeti.controller;

import com.rajneeti.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Health check controller.
 *
 * <p>Exposes a lightweight {@code GET /api/health} endpoint that confirms
 * the application is running. This endpoint is publicly accessible (no auth required).
 *
 * <p>Note: Spring Actuator also exposes {@code /actuator/health} for deeper
 * infrastructure health checks (DB connection pool, disk, etc.).
 */
@Slf4j
@RestController
@RequestMapping("/api")
public class HealthController {

    /**
     * Returns a simple UP status confirming the application is alive.
     *
     * <p>Example response:
     * <pre>
     * HTTP/1.1 200 OK
     * {
     *   "success": true,
     *   "message": "Operation successful",
     *   "data": {
     *     "status": "UP",
     *     "application": "RAJNEETI",
     *     "timestamp": "2024-01-01T00:00:00Z"
     *   }
     * }
     * </pre>
     *
     * @return 200 OK with health payload
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        log.debug("Health check requested");

        Map<String, Object> health = new LinkedHashMap<>();
        health.put("status", "UP");
        health.put("application", "RAJNEETI");
        health.put("timestamp", Instant.now().toString());

        return ResponseEntity.ok(ApiResponse.success(health));
    }
}
