---
agent: agent
description: "Phase 06 – Cloud Storage API Gateway: file upload/download metadata management"
tools: [codebase, editFiles]
---

Reference [copilot-instructions.md](../copilot-instructions.md). Phase 02 must be complete for cloud-storage service.

## Goal
The API Gateway (cloud-storage service on port 8081) handles file upload/download by coordinating metadata + chunk distribution to storage nodes. It does NOT store file data directly.

## Files to Create

### Entities (in cloud-storage service)
`entity/StorageNode.java`:
- String id (PK), String host, int port, NodeStatus status (enum: UP, DOWN, UNKNOWN), Instant lastHeartbeat

`entity/FileMetadata.java`:
- UUID id, User owner, String originalFilename, String contentType, long totalSizeBytes, int chunkCount, int replicationFactor (default 3), FileStatus status (enum: UPLOADING, COMPLETE, DEGRADED)

`entity/FileChunk.java`:
- UUID id, FileMetadata file, int chunkIndex, String nodeId, String checksum, int sizeBytes

`entity/EncryptedKey.java`:
- UUID id, FileMetadata file (OneToOne), String encryptedDek, String kekSalt, String iv

### Repositories
`repository/StorageNodeRepository.java`:
```java
List<StorageNode> findAllByStatus(NodeStatus status);
```

`repository/FileMetadataRepository.java`:
```java
List<FileMetadata> findAllByOwnerId(UUID ownerId);
```

`repository/FileChunkRepository.java`:
```java
List<FileChunk> findAllByFileIdOrderByChunkIndex(UUID fileId);
List<FileChunk> findAllByNodeId(String nodeId);
List<FileChunk> findAllByFileIdAndChunkIndex(UUID fileId, int chunkIndex);
```

### DTOs
`dto/response/FileMetadataResponse.java` — record: id, filename, contentType, sizeBytes, chunkCount, status, createdAt
`dto/response/StorageNodeResponse.java` — record: id, host, port, status
`dto/response/UploadResponse.java` — record: fileId, filename, chunkCount, replicasPerChunk

### Storage Node Client
`service/StorageNodeClient.java` (@Component, uses `RestTemplate`):
```java
void uploadChunk(String nodeId, String host, int port, UUID chunkId, byte[] encryptedData)
byte[] downloadChunk(String nodeId, String host, int port, UUID chunkId)
boolean isNodeHealthy(StorageNode node)
```

Use `RestTemplate` with timeout (connect: 2s, read: 10s).

### Node Selection Strategy
`service/NodeSelectionService.java`:
```java
// Returns `replicationFactor` healthy nodes for chunk placement
List<StorageNode> selectNodesForChunk(int replicationFactor)
```
Strategy: Round-robin across UP nodes. Throw `StorageException` if fewer than `replicationFactor` nodes available.

### Upload Service
`service/FileUploadService.java` interface:
```java
UploadResponse upload(UUID userId, MultipartFile file, String userPassword);
```

`service/impl/FileUploadServiceImpl.java`:
```java
// 1. Generate random AES-256 DEK: KeyGenerator.getInstance("AES").generateKey()
// 2. Derive KEK from userPassword using PBKDF2WithHmacSHA256 (100000 iterations, 256-bit)
// 3. Encrypt DEK with KEK using AES/GCM/NoPadding, save EncryptedKey entity
// 4. Split file bytes into 4MB chunks
// 5. For each chunk:
//    a. Encrypt chunk bytes with DEK using AES/GCM
//    b. Compute SHA-256 checksum of encrypted bytes
//    c. Select replicationFactor healthy nodes
//    d. PUT encrypted bytes to each selected node via StorageNodeClient
//    e. Save FileChunk records (one per node)
// 6. Save FileMetadata with status=COMPLETE
// 7. Return
```


### Controller
`controller/FileController.java`:
```
POST   /api/files/upload       → fileUploadService.upload() — MultipartFile + userPassword param
GET    /api/files              → list all files for authenticated user
GET    /api/files/{fileId}     → download (returns byte[] with Content-Disposition header)
DELETE /api/files/{fileId}     → delete metadata + notify nodes to delete chunks
GET    /api/admin/nodes        → list all storage nodes with status
POST   /api/admin/nodes        → register a new storage node (for admin use)
```

Upload endpoint:
```java
@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<ApiResponse<UploadResponse>> upload(
    @RequestPart("file") MultipartFile file,
    @RequestParam("password") String userPassword,
    @AuthenticationPrincipal UserPrincipal principal) { ... }
```

Download endpoint must set response headers:
```java
return ResponseEntity.ok()
    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
    .contentType(MediaType.APPLICATION_OCTET_STREAM)
    .body(fileBytes);
```

### Crypto Utility
`util/CryptoUtil.java` (@Component):
```java
// AES/GCM/NoPadding, 256-bit key, 96-bit IV
public byte[] encrypt(byte[] data, SecretKey key) throws Exception
public byte[] decrypt(byte[] encryptedData, SecretKey key) throws Exception
public SecretKey generateDek() throws Exception
public SecretKey deriveKek(String password, byte[] salt) throws Exception
public byte[] encryptKey(SecretKey dek, SecretKey kek) throws Exception
public SecretKey decryptKey(byte[] encryptedDek, SecretKey kek) throws Exception
```
- Use `IvParameterSpec` with a random 12-byte IV prepended to the ciphertext

## Done When
- [ ] `POST /api/files/upload` with a test file returns `UploadResponse` with chunkCount
- [ ] File bytes are distributed across storage node containers (check node volumes)
- [ ] `GET /api/files/{id}` returns the original file correctly (same bytes)
- [ ] Storage nodes only receive opaque encrypted bytes (cannot read file content)
- [ ] Wrong password on download fails with 403