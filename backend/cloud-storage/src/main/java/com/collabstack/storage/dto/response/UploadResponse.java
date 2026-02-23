package com.collabstack.storage.dto.response;

import java.util.UUID;

public record UploadResponse(
        UUID fileId,
        String filename,
        int chunkCount,
        int replicasPerChunk
) {}
