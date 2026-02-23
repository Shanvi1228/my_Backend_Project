package com.collabstack.storage.repository;

import com.collabstack.storage.entity.FileChunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FileChunkRepository extends JpaRepository<FileChunk, UUID> {
    List<FileChunk> findAllByFile_IdOrderByChunkIndex(UUID fileId);
    List<FileChunk> findAllByNodeId(String nodeId);
    List<FileChunk> findAllByFile_IdAndChunkIndex(UUID fileId, int chunkIndex);
    void deleteAllByFile_Id(UUID fileId);
}
