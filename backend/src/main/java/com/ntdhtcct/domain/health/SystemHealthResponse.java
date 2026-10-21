package com.ntdhtcct.domain.health;

import java.time.OffsetDateTime;
import java.util.Map;

public record SystemHealthResponse(
        String status,
        OffsetDateTime checkedAt,
        Map<String, ServiceStatus> services
) {
    public record ServiceStatus(String status, String message) {
    }
}
