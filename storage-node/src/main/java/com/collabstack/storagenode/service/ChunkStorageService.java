package com.collabstack.storagenode.service;

import com.collabstack.storagenode.config.StorageConfig;
import com.collabstack.storagenode.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChunkStorageService {

    private final StorageConfig storageConfig;

    public void store(String chunkId, byte[] encryptedData) {
        Path target = chunkPath(chunkId);
        try {
            Files.write(target, encryptedData);
            log.debug("Stored chunk {} ({} bytes)", chunkId, encryptedData.length);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store chunk: " + chunkId, e);
        }
    }

    public byte[] retrieve(String chunkId) {
        Path target = chunkPath(chunkId);
        if (!Files.exists(target)) {
            throw new ResourceNotFoundException("Chunk not found: " + chunkId);
        }
        try {
            return Files.readAllBytes(target);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read chunk: " + chunkId, e);
        }
    }

    public void delete(String chunkId) {
        try {
            Files.deleteIfExists(chunkPath(chunkId));
            log.debug("Deleted chunk {}", chunkId);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to delete chunk: " + chunkId, e);
        }
    }

    public boolean exists(String chunkId) {
        return Files.exists(chunkPath(chunkId));
    }

    public long countChunks() {
        Path dir = Path.of(storageConfig.getDataDir());
        if (!Files.exists(dir)) return 0;
        try {
            return Files.list(dir)
                    .filter(p -> p.toString().endsWith(".enc"))
                    .count();
        } catch (IOException e) {
            return 0;
        }
    }

    private Path chunkPath(String chunkId) {
        return Path.of(storageConfig.getDataDir(), chunkId + ".enc");
    }
}
