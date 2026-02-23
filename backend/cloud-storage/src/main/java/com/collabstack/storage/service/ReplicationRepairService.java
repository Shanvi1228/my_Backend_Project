package com.collabstack.storage.service;

import com.collabstack.storage.entity.*;
import com.collabstack.storage.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReplicationRepairService {

    private final StorageNodeRepository storageNodeRepository;
    private final FileChunkRepository fileChunkRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final StorageNodeClient storageNodeClient;

    @Scheduled(fixedDelayString = "${storage.replication.repair-interval-ms:30000}")
    @Transactional
    public void repairUnderReplicatedChunks() {
        // Strategy: scan ALL chunks grouped by (fileId, chunkIndex) and top up
        // any that have fewer healthy replicas than the file's replication factor.
        // This handles both: (a) node went DOWN losing replicas, and
        // (b) chunk was never placed on enough nodes during upload.

        List<FileChunk> allChunks = fileChunkRepository.findAll();
        if (allChunks.isEmpty()) return;

        // Group by fileId + chunkIndex key
        Map<String, List<FileChunk>> grouped = allChunks.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getFile().getId() + "_" + c.getChunkIndex()));

        int repairedCount = 0;
        for (Map.Entry<String, List<FileChunk>> entry : grouped.entrySet()) {
            if (repairChunkGroup(entry.getValue())) repairedCount++;
        }

        if (repairedCount > 0) {
            log.info("Repair cycle complete: {} chunk groups repaired", repairedCount);
        }
    }

    private boolean repairChunkGroup(List<FileChunk> chunksForIndex) {
        UUID fileId = chunksForIndex.get(0).getFile().getId();
        int chunkIndex = chunksForIndex.get(0).getChunkIndex();
        int replicationFactor = chunksForIndex.get(0).getFile().getReplicationFactor();

        List<FileChunk> allReplicas = fileChunkRepository.findAllByFile_IdAndChunkIndex(fileId, chunkIndex);

        long healthyReplicas = allReplicas.stream()
                .filter(c -> storageNodeRepository.findById(c.getNodeId())
                        .map(n -> n.getStatus() == NodeStatus.UP)
                        .orElse(false))
                .count();

        int needed = (int) (replicationFactor - healthyReplicas);
        if (needed <= 0) return false;

        // Find a healthy source replica
        FileChunk sourceChunk = allReplicas.stream()
                .filter(c -> storageNodeRepository.findById(c.getNodeId())
                        .map(n -> n.getStatus() == NodeStatus.UP)
                        .orElse(false))
                .findFirst()
                .orElse(null);

        if (sourceChunk == null) {
            log.error("CRITICAL: No healthy replica for file {} chunk {} — data may be lost!", fileId, chunkIndex);
            fileMetadataRepository.findById(fileId).ifPresent(f -> {
                f.setStatus(FileStatus.DEGRADED);
                fileMetadataRepository.save(f);
            });
            return false;
        }

        // Download from source using the storage node file key
        StorageNode sourceNode = storageNodeRepository.findById(sourceChunk.getNodeId()).orElseThrow();
        byte[] chunkData = storageNodeClient.downloadChunk(
                sourceNode.getId(), sourceNode.getHost(), sourceNode.getPort(),
                sourceChunk.getChunkStorageId());

        // Find new target nodes not already holding this chunk
        Set<String> existingNodeIds = allReplicas.stream()
                .map(FileChunk::getNodeId)
                .collect(Collectors.toSet());

        List<StorageNode> candidates = storageNodeRepository.findAllByStatus(NodeStatus.UP).stream()
                .filter(n -> !existingNodeIds.contains(n.getId()))
                .limit(needed)
                .toList();

        for (StorageNode target : candidates) {
            UUID newChunkId = UUID.randomUUID();
            storageNodeClient.uploadChunk(target.getId(), target.getHost(), target.getPort(), newChunkId, chunkData);
            FileChunk newChunk = FileChunk.builder()
                    .file(chunksForIndex.get(0).getFile())
                    .chunkIndex(chunkIndex)
                    .nodeId(target.getId())
                    .chunkStorageId(newChunkId)
                    .checksum(sourceChunk.getChecksum())
                    .sizeBytes(chunkData.length)
                    .build();
            fileChunkRepository.save(newChunk);
            log.info("Repaired chunk {} of file {} → node {}", chunkIndex, fileId, target.getId());
        }
        return !candidates.isEmpty();
    }
}
