package com.collabstack.editor.service;

import com.collabstack.editor.dto.websocket.OperationMessage;
import com.collabstack.editor.entity.Document;
import com.collabstack.editor.entity.DocumentOperation;
import com.collabstack.editor.repository.DocumentOperationRepository;
import com.collabstack.editor.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OperationPersistenceService {

    private final DocumentOperationRepository operationRepository;
    private final DocumentRepository documentRepository;

    // Optional — only injected when EmbeddingService bean is available
    @Autowired(required = false)
    private EmbeddingService embeddingService;

    /**
     * Asynchronously persists a single operation to the document_operations table.
     */
    @Async
    @Transactional
    public void persistOperation(UUID documentId, UUID userId, OperationMessage op, long revision) {
        try {
            DocumentOperation operation = DocumentOperation.builder()
                    .documentId(documentId)
                    .userId(userId)
                    .opType(op.opType() != null ? op.opType().name() : "UNKNOWN")
                    .position(op.position())
                    .content(op.content())
                    .length(op.length())
                    .revision(revision)
                    .build();
            operationRepository.save(operation);
        } catch (Exception e) {
            log.error("Failed to persist operation for document {}: {}", documentId, e.getMessage());
        }
    }

    /**
     * Asynchronously updates documents.content_snapshot when all clients disconnect.
     * Also triggers re-indexing of the document for RAG.
     */
    @Async
    @Transactional
    public void saveSnapshot(UUID documentId, String content, long revision) {
        try {
            Document document = documentRepository.findById(documentId).orElse(null);
            if (document != null) {
                document.setContentSnapshot(content);
                document.setCurrentRevision(revision);
                documentRepository.save(document);
                log.info("Snapshot saved for document {} at revision {}", documentId, revision);
            }
        } catch (Exception e) {
            log.error("Failed to save snapshot for document {}: {}", documentId, e.getMessage());
        }
        // Re-index for RAG after snapshot is persisted
        if (embeddingService != null && content != null && !content.isBlank()) {
            embeddingService.indexDocument(documentId, content);
        }
    }
}
