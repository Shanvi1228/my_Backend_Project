---
agent: agent
description: "Phase 04 – WebSocket real-time collaboration with OT-style operation sync"
tools: [codebase, editFiles]
---

Reference [copilot-instructions.md](../copilot-instructions.md). Phase 03 must be complete.

## Goal
Multi-user real-time editing via WebSocket. Operations are broadcast to all collaborators in a document room. Server maintains canonical document state.

## Architecture
- Client connects to `/ws/documents/{docId}` via WebSocket (raw WebSocket, NOT STOMP for simplicity — but configure STOMP as fallback option)
- On connect: server validates JWT from query param `?token=`, verifies user has access to docId
- Client sends `OperationMessage` JSON; server applies it, updates revision, broadcasts to others
- Server keeps an in-memory `Map<UUID, DocumentSession>` for active document sessions

## Files to Create

### DTO / Message Types
`dto/websocket/OperationType.java` — enum: INSERT, DELETE

`dto/websocket/OperationMessage.java`:
```java
public record OperationMessage(
    String type,            // "OPERATION" | "PRESENCE" | "SYNC"
    OperationType opType,   // INSERT or DELETE
    int position,
    String content,         // for INSERT
    int length,             // for DELETE
    long clientRevision,    // revision client thinks it's at
    String userId,
    String username
) {}
```

`dto/websocket/SyncMessage.java` — sent to new client on connect:
```java
public record SyncMessage(
    String type,            // always "SYNC"
    String content,         // current full document content
    long revision
) {}
```

`dto/websocket/PresenceMessage.java` — sent when user joins/leaves:
```java
public record PresenceMessage(
    String type,            // "PRESENCE"
    String userId,
    String username,
    String event            // "JOIN" | "LEAVE"
) {}
```

### Session Management
`websocket/DocumentSession.java`:
```java
public class DocumentSession {
    private final UUID documentId;
    private String currentContent;
    private long revision;
    private final ConcurrentHashMap<String, WebSocketSession> activeSessions;
    // thread-safe methods: applyOperation(), addSession(), removeSession(), broadcastToOthers()
}
```

`websocket/CollaborationSessionManager.java` (@Component):
```java
// ConcurrentHashMap<UUID, DocumentSession> sessions
public DocumentSession getOrCreate(UUID documentId, String initialContent, long revision)
public void removeSession(UUID documentId, String sessionId)
```

### WebSocket Handler
`websocket/DocumentWebSocketHandler.java extends TextWebSocketHandler`:
- `afterConnectionEstablished`: extract JWT from URI query param, validate, get/create DocumentSession, add session, send SyncMessage to new client, broadcast PresenceMessage("JOIN") to others
- `handleTextMessage`: parse OperationMessage, apply to DocumentSession, persist to `document_operations` table asynchronously, broadcast to all OTHER sessions
- `afterConnectionClosed`: remove from session, broadcast PresenceMessage("LEAVE"), if session empty → persist final snapshot to `documents` table

### OT Logic (simple, server-authoritative)
In `DocumentSession.applyOperation()`:
```
if clientRevision == serverRevision:
    apply operation directly
else if clientRevision < serverRevision:
    transform operation against ops since clientRevision
    (for MVP: just apply as-is — document note in README that full OT is a future improvement)
    apply transformed operation
increment revision
persist operation async
```

### WebSocket Config
`config/WebSocketConfig.java`:
```java
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(documentWebSocketHandler, "/ws/documents/{docId}")
                .setAllowedOriginPatterns("*");
    }
}
```

### Async Persistence
`service/OperationPersistenceService.java`:
- `@Async` method: `persistOperation(UUID docId, UUID userId, OperationMessage op, long revision)`
- Saves to `document_operations` table
- `saveSnapshot(UUID docId, String content, long revision)` — updates `documents` table

Add `@EnableAsync` to `CollabEditorApplication.java`.

## Done When
- [ ] Two browser tabs open same `/documents/{id}`, typing in one appears in the other within 200ms
- [ ] On new connection, client receives full current document content via SyncMessage
- [ ] Presence events (JOIN/LEAVE) are broadcast to all connected clients
- [ ] Operations are persisted to `document_operations` table
- [ ] Snapshot is saved to `documents.content_snapshot` when all clients disconnect
- [ ] Invalid JWT on WebSocket connect causes immediate close with code 1008