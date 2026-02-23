package com.collabstack.storage.dto.response;

import com.collabstack.storage.entity.FileStatus;

import java.time.Instant;
import java.util.UUID;

public record FileMetadataResponse(
        UUID id,
        String filename,
        String contentType,
        long sizeBytes,
        int chunkCount,
        FileStatus status,
        Instant createdAt
) {}
