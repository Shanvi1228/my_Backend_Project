---
agent: agent
description: "Phase 08 – Node health monitoring, replication repair, and admin dashboard API"
tools: [codebase, editFiles]
---

Reference [copilot-instructions.md](../copilot-instructions.md). Phase 07 must be complete.

## Goal
Add automatic health monitoring, replication factor enforcement, and a repair daemon that restores lost replicas when a node goes down. This is the system's fault-tolerance core.

## Files to Create / Modify (in cloud-storage service)

### Node Registration on Startup
`service/NodeRegistrationService.java` (@Component):
- `@EventListener(ApplicationReadyEvent.class)` — on startup, read storage node URLs from config and call each node's `/chunks/health`
- For each responding node: upsert `StorageNode` in DB with status=UP
- For non-responding nodes: set status=UNKNOWN
- Log results

`config/StorageNodeProperties.java`:
```java
@Configuration
@ConfigurationProperties(prefix = "storage.nodes")
@Data
public class StorageNodeProperties {
    // List of {id, host, port} read from application.yml
    private List<NodeConfig> nodes;

    @Data
    public static class NodeConfig {
        private String id;
        private String host;
        private int port;
    }
}
```

Add to `application.yml`:
```yaml
storage:
  nodes:
    nodes:
      - id: node-1
        host: ${NODE1_HOST:localhost}
        port: 9001
      - id: node-2
        host: ${NODE2_HOST:localhost}
        port: 9002
      - id: node-3
        host: ${NODE3_HOST:localhost}
        port: 9003
  replication:
    factor: 3
    health-check-interval-ms: 10000
    repair-interval-ms: 30000
```

### Health Monitor
`service/NodeHealthMonitor.java` (@Component):
```java
@Scheduled(fixedDelayString = "${storage.replication.health-check-interval-ms:10000}")
public void checkAllNodes() {
    List<StorageNode> allNodes = storageNodeRepository.findAll();
    for (StorageNode node : allNodes) {
        boolean healthy = storageNodeClient.isNodeHealthy(node);
        NodeStatus newStatus = healthy ? NodeStatus.UP : NodeStatus.DOWN;
        if (node.getStatus() != newStatus) {
            log.warn("Node {} status changed: {} → {}", node.getId(), node.getStatus(), newStatus);
        }
        node.setStatus(newStatus);
        node.setLastHeartbeat(healthy ? Instant.now() : node.getLastHeartbeat());
        storageNodeRepository.save(node);
    }
}
```

Add `@EnableScheduling` to `CloudStorageApplication.java`.

### Repair Daemon
`service/ReplicationRepairService.java` (@Component):
```java
@Scheduled(fixedDelayString = "${storage.replication.repair-interval-ms:30000}")
public void repairUnderReplicatedChunks() {
    // 1. Find all DOWN nodes
    List<StorageNode> downNodes = storageNodeRepository.findAllByStatus(NodeStatus.DOWN);
    if (downNodes.isEmpty()) return;

    for (StorageNode downNode : downNodes) {
        List<FileChunk> affectedChunks = fileChunkRepository.findAllByNodeId(downNode.getId());
        log.info("Repairing {} chunks from down node {}", affectedChunks.size(), downNode.getId());

        // Group by (fileId, chunkIndex)
        Map<String, List<FileChunk>> grouped = affectedChunks.stream()
            .collect(Collectors.groupingBy(c -> c.getFile().getId() + "_" + c.getChunkIndex()));

        for (Map.Entry<String, List<FileChunk>> entry : grouped.entrySet()) {
            repairChunkGroup(entry.getKey(), entry.getValue());
        }
    }
}

private void repairChunkGroup(String key, List<FileChunk> chunksForIndex) {
    // 2. Check current healthy replica count for this chunk
    UUID fileId = chunksForIndex.get(0).getFile().getId();
    int chunkIndex = chunksForIndex.get(0).getChunkIndex();

    List<FileChunk> allReplicas = fileChunkRepository.findAllByFileIdAndChunkIndex(fileId, chunkIndex);
    long healthyReplicas = allReplicas.stream()
        .filter(c -> storageNodeRepository.findById(c.getNodeId())
            .map(n -> n.getStatus() == NodeStatus.UP).orElse(false))
        .count();

    int replicationFactor = chunksForIndex.get(0).getFile().getReplicationFactor();
    int needed = (int)(replicationFactor - healthyReplicas);
    if (needed <= 0) return;

    // 3. Find a healthy source replica to copy from
    FileChunk sourceChunk = allReplicas.stream()
        .filter(c -> storageNodeRepository.findById(c.getNodeId())
            .map(n -> n.getStatus() == NodeStatus.UP).orElse(false))
        .findFirst()
        .orElse(null);

    if (sourceChunk == null) {
        log.error("CRITICAL: No healthy replica for file {} chunk {} — data may be lost!", fileId, chunkIndex);
        fileMetadataRepository.findById(fileId).ifPresent(f -> {
            f.setStatus(FileStatus.DEGRADED);
            fileMetadataRepository.save(f);
        });
        return;
    }

    // 4. Download from source
    StorageNode sourceNode = storageNodeRepository.findById(sourceChunk.getNodeId()).orElseThrow();
    byte[] chunkData = storageNodeClient.downloadChunk(
        sourceNode.getId(), sourceNode.getHost(), sourceNode.getPort(), UUID.fromString(sourceChunk.getId().toString()));

    // 5. Find `needed` new healthy target nodes (exclude nodes already having this chunk)
    Set<String> existingNodeIds = allReplicas.stream().map(FileChunk::getNodeId).collect(Collectors.toSet());
    List<StorageNode> candidates = storageNodeRepository.findAllByStatus(NodeStatus.UP).stream()
        .filter(n -> !existingNodeIds.contains(n.getId()))
        .limit(needed)
        .toList();

    // 6. Upload to each target and create new FileChunk records
    for (StorageNode target : candidates) {
        UUID newChunkId = UUID.randomUUID();
        storageNodeClient.uploadChunk(target.getId(), target.getHost(), target.getPort(), newChunkId, chunkData);
        FileChunk newChunk = FileChunk.builder()
            .file(chunksForIndex.get(0).getFile())
            .chunkIndex(chunkIndex)
            .nodeId(target.getId())
            .checksum(sourceChunk.getChecksum())
            .sizeBytes(chunkData.length)
            .build();
        fileChunkRepository.save(newChunk);
        log.info("Repaired chunk {} of file {} on node {}", chunkIndex, fileId, target.getId());
    }
}
```

### Admin API Endpoints
Add to `FileController.java`:
```java
@GetMapping("/admin/nodes")
public ResponseEntity<ApiResponse<List<StorageNodeResponse>>> getNodes() { ... }

@GetMapping("/admin/files/{fileId}/chunks")
public ResponseEntity<ApiResponse<Map<String, Object>>> getChunkMap(@PathVariable UUID fileId) {
    // Return: {fileId, chunks: [{chunkIndex, replicas: [{nodeId, status, checksum}]}]}
}

@PostMapping("/admin/repair")
public ResponseEntity<ApiResponse<String>> triggerRepair() {
    replicationRepairService.repairUnderReplicatedChunks();
    return ResponseEntity.ok(ApiResponse.ok("Repair job triggered", null));
}
```

## Fault Tolerance Demo Script
Create `scripts/demo-fault-tolerance.sh`:
```bash
#!/bin/bash
echo "=== Uploading test file ==="
FILE_ID=$(curl -s -X POST http://localhost:8081/api/files/upload \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -F "file=@test-file.txt" \
  -F "password=testpassword" | jq -r '.data.fileId')
echo "Uploaded fileId: $FILE_ID"

echo "=== Checking replicas ==="
curl -s http://localhost:8081/api/admin/files/$FILE_ID/chunks | jq .

echo "=== Stopping node-2 ==="
docker compose stop storage-node-2

sleep 15  # wait for health check to detect DOWN

echo "=== Node status after failure ==="
curl -s http://localhost:8081/api/admin/nodes | jq .

echo "=== Triggering repair ==="
curl -s -X POST http://localhost:8081/api/admin/repair \
  -H "Authorization: Bearer $JWT_TOKEN" | jq .

sleep 5

echo "=== Replicas after repair ==="
curl -s http://localhost:8081/api/admin/files/$FILE_ID/chunks | jq .

echo "=== Downloading file despite node-2 being down ==="
curl -s -o downloaded-file.txt \
  http://localhost:8081/api/files/$FILE_ID?password=testpassword \
  -H "Authorization: Bearer $JWT_TOKEN"
echo "Download successful: $(wc -c downloaded-file.txt)"
```

## Done When
- [ ] Health monitor updates node status every 10s
- [ ] Stopping a storage node container causes its DB status to flip to DOWN
- [ ] Repair daemon detects under-replicated chunks and re-replicates to remaining UP nodes
- [ ] File can still be downloaded even with one node down (as long as 2/3 up)
- [ ] File status is set to DEGRADED if no healthy replica exists
- [ ] `GET /api/admin/files/{id}/chunks` clearly shows per-chunk replica distribution
- [ ] Demo script runs end-to-end successfully