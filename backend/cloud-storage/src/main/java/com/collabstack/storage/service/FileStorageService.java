package com.collabstack.storage.service;

import com.collabstack.storage.dto.response.FileMetadataResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface FileStorageService {
    List<FileMetadataResponse> listFiles(UUID userId);
    byte[] downloadFile(UUID fileId, UUID userId, String userPassword);
    String getFilename(UUID fileId, UUID userId);
    void deleteFile(UUID fileId, UUID userId);
    Map<String, Object> getChunkMap(UUID fileId);
}

