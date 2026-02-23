package com.collabstack.editor.repository;

import com.collabstack.editor.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, UUID> {
    List<DocumentChunk> findAllByDocumentIdOrderByChunkIndexAsc(UUID documentId);
    void deleteAllByDocumentId(UUID documentId);
}
