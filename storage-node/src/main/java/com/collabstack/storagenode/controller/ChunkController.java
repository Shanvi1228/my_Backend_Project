package com.collabstack.storagenode.controller;

import com.collabstack.storagenode.config.StorageConfig;
import com.collabstack.storagenode.dto.ChunkUploadResponse;
import com.collabstack.storagenode.dto.HealthResponse;
import com.collabstack.storagenode.service.ChunkStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/chunks")
@RequiredArgsConstructor
public class ChunkController {

    private final ChunkStorageService storageService;
    private final StorageConfig storageConfig;

    @PutMapping(value = "/{chunkId}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<ChunkUploadResponse> storeChunk(
            @PathVariable String chunkId,
            @RequestBody byte[] data) {
        storageService.store(chunkId, data);
        return ResponseEntity.ok(new ChunkUploadResponse(chunkId, storageConfig.getId(), data.length));
    }

    @GetMapping(value = "/{chunkId}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> getChunk(@PathVariable String chunkId) {
        byte[] data = storageService.retrieve(chunkId);
        return ResponseEntity.ok(data);
    }

    @DeleteMapping("/{chunkId}")
    public ResponseEntity<Void> deleteChunk(@PathVariable String chunkId) {
        storageService.delete(chunkId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(new HealthResponse(
                storageConfig.getId(),
                "UP",
                storageService.countChunks(),
                storageConfig.getDataDir(),
                Instant.now()
        ));
    }
}
