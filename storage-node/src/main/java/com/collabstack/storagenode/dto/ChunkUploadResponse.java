package com.collabstack.storagenode.dto;

public record ChunkUploadResponse(
        String chunkId,
        String nodeId,
        int sizeBytes
) {}
