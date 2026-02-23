package com.collabstack.editor.websocket;

import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CollaborationSessionManager {

    private final ConcurrentHashMap<UUID, DocumentSession> sessions = new ConcurrentHashMap<>();

    /**
     * Returns existing DocumentSession or creates a new one seeded with initialContent/revision.
     */
    public DocumentSession getOrCreate(UUID documentId, String initialContent, long revision) {
        return sessions.computeIfAbsent(documentId,
                id -> new DocumentSession(id, initialContent, revision));
    }

    /**
     * Gets an existing session; returns null if none exists.
     */
    public DocumentSession get(UUID documentId) {
        return sessions.get(documentId);
    }

    /**
     * Removes the document session entirely (called when last user disconnects).
     */
    public void removeDocumentSession(UUID documentId) {
        sessions.remove(documentId);
    }
}
