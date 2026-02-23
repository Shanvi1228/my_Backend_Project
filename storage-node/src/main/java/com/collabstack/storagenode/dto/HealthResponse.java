package com.collabstack.storagenode.dto;

import java.time.Instant;

public record HealthResponse(
        String nodeId,
        String status,
        long totalChunks,
        String dataDir,
        Instant timestamp
) {}
