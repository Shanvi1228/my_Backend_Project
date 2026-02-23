---
agent: agent
description: "Phase 07 – Storage Node Service: chunk store/retrieve REST endpoints + health"
tools: [codebase, editFiles]
---

Reference [copilot-instructions.md](../copilot-instructions.md). Phase 06 must be complete.

## Goal
Build the `storage-node` Spring Boot app. Each node stores encrypted chunk files on disk and exposes simple REST endpoints. Three instances run simultaneously via Docker Compose.

## Important
Storage nodes have NO database, NO authentication, and NO knowledge of files or users.
They are dumb blob stores — they only know about chunkIds and bytes.

## Files to Create

### Config
`config/StorageConfig.java`:
```java
@Configuration
@ConfigurationProperties(prefix = "storage.node")
@Data
public class StorageConfig {
    private String id;          // e.g. "node-1"
    private String dataDir;     // e.g. "./data/chunks"
}
```

`StorageNodeApplication.java` — annotate with `@SpringBootApplication` and also:
```java
// On startup, create the dataDir if it doesn't exist
@Bean
CommandLineRunner init(StorageConfig config) {
    return args -> {
        Path dir = Path.of(config.getDataDir());
        if (!Files.exists(dir)) Files.createDirectories(dir);
        System.out.println("Storage node [" + config.getId() + "] ready at: " + dir.toAbsolutePath());
    };
}
```

### DTOs
`dto/ChunkUploadResponse.java` — record: chunkId, nodeId, sizeBytes
`dto/HealthResponse.java` — record: nodeId, status, totalChunks, dataDir, timestamp

### Service
`service/ChunkStorageService.java` (@Service):
```java
// Store encrypted bytes to disk as: {dataDir}/{chunkId}.enc
void store(String chunkId, byte[] encryptedData)

// Read encrypted bytes from disk
byte[] retrieve(String chunkId)

// Delete chunk from disk
void delete(String chunkId)

// Check if chunk exists
boolean exists(String chunkId)

// Count total chunks stored
long countChunks()
```

Implementation details:
- `store`: write using `Files.write(Path.of(dataDir, chunkId + ".enc"), encryptedData)`
- `retrieve`: read using `Files.readAllBytes(...)`. Throw `ResourceNotFoundException` if file doesn't exist
- `delete`: `Files.deleteIfExists(...)`
- `countChunks`: `Files.list(Path.of(dataDir)).count()`

### Controller
`controller/ChunkController.java`:
```java
@RestController
@RequestMapping("/chunks")
@RequiredArgsConstructor
public class ChunkController {

    private final ChunkStorageService storageService;
    private final StorageConfig storageConfig;

    // Store a chunk
    @PutMapping(value = "/{chunkId}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<ChunkUploadResponse> storeChunk(
        @PathVariable String chunkId,
        @RequestBody byte[] data) {
        storageService.store(chunkId, data);
        return ResponseEntity.ok(new ChunkUploadResponse(chunkId, storageConfig.getId(), data.length));
    }

    // Retrieve a chunk
    @GetMapping(value = "/{chunkId}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> getChunk(@PathVariable String chunkId) {
        byte[] data = storageService.retrieve(chunkId);
        return ResponseEntity.ok(data);
    }

    // Delete a chunk
    @DeleteMapping("/{chunkId}")
    public ResponseEntity<Void> deleteChunk(@PathVariable String chunkId) {
        storageService.delete(chunkId);
        return ResponseEntity.noContent().build();
    }

    // Health check
    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(new HealthResponse(
            storageConfig.getId(), "UP",
            storageService.countChunks(),
            storageConfig.getDataDir(),
            Instant.now()
        ));
    }
}
```

### Exception Handling
`exception/ResourceNotFoundException.java`:
```java
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String msg) { super(msg); }
}
```

`exception/StorageNodeExceptionHandler.java` (@RestControllerAdvice):
- `ResourceNotFoundException` → 404 `{error: "Chunk not found"}`
- `Exception` → 500 `{error: "Storage node internal error"}`

### Spring Boot Config (application.yml)
Already created in Phase 01. Verify these properties exist:
```yaml
server:
  port: ${NODE_PORT:9001}
spring:
  servlet:
    multipart:
      max-file-size: 10MB        # individual chunk max 4MB + overhead
      max-request-size: 10MB
storage:
  node:
    id: ${NODE_ID:node-1}
    data-dir: ${DATA_DIR:./data/chunks}
```

## Validation Steps
- [ ] `curl -X PUT http://localhost:9001/chunks/test-chunk-001 --data-binary @somefile.txt -H "Content-Type: application/octet-stream"` → 200
- [ ] `curl http://localhost:9001/chunks/test-chunk-001` → returns same bytes
- [ ] `curl http://localhost:9001/chunks/health` → `{nodeId: "node-1", status: "UP", totalChunks: 1}`
- [ ] Non-existent chunk GET → 404
- [ ] All three Docker nodes (9001, 9002, 9003) respond to `/chunks/health` independently

## Done When
- [ ] All 3 storage node containers store/retrieve chunks independently
- [ ] Each container's `/data/chunks/` volume persists across container restarts
- [ ] StorageNodeClient in cloud-storage service can successfully PUT and GET to all three nodes