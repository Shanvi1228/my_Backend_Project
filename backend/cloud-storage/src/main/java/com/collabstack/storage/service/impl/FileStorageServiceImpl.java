package com.collabstack.storage.service.impl;

import com.collabstack.storage.dto.response.FileMetadataResponse;
import com.collabstack.storage.entity.*;
import com.collabstack.storage.exception.ResourceNotFoundException;
import com.collabstack.storage.exception.StorageException;
import com.collabstack.storage.exception.UnauthorizedException;
import com.collabstack.storage.repository.*;
import com.collabstack.storage.service.FileStorageService;
import com.collabstack.storage.service.StorageNodeClient;
import com.collabstack.storage.util.CryptoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageServiceImpl implements FileStorageService {

    private final FileMetadataRepository fileMetadataRepository;
    private final FileChunkRepository fileChunkRepository;
    private final EncryptedKeyRepository encryptedKeyRepository;
    private final StorageNodeRepository storageNodeRepository;
    private final StorageNodeClient storageNodeClient;
    private final CryptoUtil cryptoUtil;

    @Override
    @Transactional(readOnly = true)
    public List<FileMetadataResponse> listFiles(UUID userId) {
        return fileMetadataRepository.findAllByOwnerId(userId).stream()
                .map(f -> new FileMetadataResponse(
                        f.getId(), f.getOriginalFilename(), f.getContentType(),
                        f.getTotalSizeBytes(), f.getChunkCount(), f.getStatus(), f.getCreatedAt()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] downloadFile(UUID fileId, UUID userId, String userPassword) {
        FileMetadata metadata = getFileOrThrow(fileId);
        assertOwner(metadata, userId);

        EncryptedKey keyRecord = encryptedKeyRepository.findByFile_Id(fileId)
                .orElseThrow(() -> new StorageException("Encryption key not found for file: " + fileId));

        // Derive KEK from password
        byte[] kekSalt = Base64.getDecoder().decode(keyRecord.getKekSalt());
        SecretKey kek;
        try {
            kek = cryptoUtil.deriveKek(userPassword, kekSalt);
        } catch (Exception e) {
            throw new StorageException("Failed to derive key: " + e.getMessage());
        }

        // Decrypt DEK
        byte[] encryptedDekBytes = Base64.getDecoder().decode(keyRecord.getEncryptedDek());
        SecretKey dek;
        try {
            dek = cryptoUtil.decryptKey(encryptedDekBytes, kek);
        } catch (Exception e) {
            // AEADBadTagException means wrong password
            throw new UnauthorizedException("Invalid password — cannot decrypt file");
        }

        // Download and decrypt each chunk
        List<FileChunk> chunks = fileChunkRepository.findAllByFile_IdOrderByChunkIndex(fileId);
        // Group by chunkIndex, use first successful replica
        Map<Integer, List<FileChunk>> byIndex = new LinkedHashMap<>();
        for (FileChunk chunk : chunks) {
            byIndex.computeIfAbsent(chunk.getChunkIndex(), k -> new ArrayList<>()).add(chunk);
        }

        ByteArrayOutputStream assembled = new ByteArrayOutputStream();
        for (int i = 0; i < metadata.getChunkCount(); i++) {
            List<FileChunk> replicas = byIndex.getOrDefault(i, Collections.emptyList());
            byte[] decryptedChunk = downloadChunkWithFallback(i, replicas, dek);
            try {
                assembled.write(decryptedChunk);
            } catch (IOException e) {
                throw new StorageException("Failed to assemble chunk " + i);
            }
        }

        log.info("File {} downloaded successfully ({} chunks)", fileId, metadata.getChunkCount());
        return assembled.toByteArray();
    }

    @Override
    @Transactional(readOnly = true)
    public String getFilename(UUID fileId, UUID userId) {
        FileMetadata metadata = getFileOrThrow(fileId);
        assertOwner(metadata, userId);
        return metadata.getOriginalFilename();
    }

    @Override
    @Transactional
    public void deleteFile(UUID fileId, UUID userId) {
        FileMetadata metadata = getFileOrThrow(fileId);
        assertOwner(metadata, userId);

        // Notify each storage node to delete its chunks
        List<FileChunk> chunks = fileChunkRepository.findAllByFile_IdOrderByChunkIndex(fileId);
        for (FileChunk chunk : chunks) {
            storageNodeRepository.findById(chunk.getNodeId()).ifPresent(node ->
                storageNodeClient.deleteChunk(node.getId(), node.getHost(), node.getPort(),
                        chunk.getChunkStorageId()));
        }

        encryptedKeyRepository.deleteByFile_Id(fileId);
        fileChunkRepository.deleteAllByFile_Id(fileId);
        fileMetadataRepository.delete(metadata);
        log.info("File {} deleted by user {}", fileId, userId);
    }

    private byte[] downloadChunkWithFallback(int chunkIndex, List<FileChunk> replicas, SecretKey dek) {
        // Try UP nodes first, then fall back to any remaining node
        List<FileChunk> ordered = replicas.stream()
                .sorted((a, b) -> {
                    NodeStatus sa = storageNodeRepository.findById(a.getNodeId())
                            .map(StorageNode::getStatus).orElse(NodeStatus.UNKNOWN);
                    NodeStatus sb = storageNodeRepository.findById(b.getNodeId())
                            .map(StorageNode::getStatus).orElse(NodeStatus.UNKNOWN);
                    // UP first (ordinal 0=UNKNOWN,1=UP,2=DOWN — sort descending by UP)
                    return Boolean.compare(sb == NodeStatus.UP, sa == NodeStatus.UP);
                })
                .toList();

        for (FileChunk replica : ordered) {
            Optional<StorageNode> nodeOpt = storageNodeRepository.findById(replica.getNodeId());
            if (nodeOpt.isEmpty()) {
                log.warn("Node {} not found in DB, skipping chunk {}", replica.getNodeId(), chunkIndex);
                continue;
            }
            StorageNode node = nodeOpt.get();
            if (node.getStatus() == NodeStatus.DOWN) {
                log.debug("Skipping DOWN node {} for chunk {}", node.getId(), chunkIndex);
                continue;
            }
            try {
                byte[] encryptedChunk = storageNodeClient.downloadChunk(
                        node.getId(), node.getHost(), node.getPort(), replica.getChunkStorageId());
                return cryptoUtil.decrypt(encryptedChunk, dek);
            } catch (Exception e) {
                log.warn("Failed to download/decrypt chunk {} from node {}: {}",
                        chunkIndex, node.getId(), e.getMessage());
            }
        }
        throw new StorageException("All replicas failed for chunk " + chunkIndex);
    }

    private FileMetadata getFileOrThrow(UUID fileId) {
        return fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found: " + fileId));
    }

    private void assertOwner(FileMetadata metadata, UUID userId) {
        // Use a DB-level check to avoid triggering lazy proxy initialisation
        if (!fileMetadataRepository.existsByIdAndOwner_Id(metadata.getId(), userId)) {
            throw new UnauthorizedException("You do not have access to this file");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getChunkMap(UUID fileId) {
        FileMetadata metadata = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found: " + fileId));

        List<FileChunk> allChunks = fileChunkRepository.findAllByFile_IdOrderByChunkIndex(fileId);

        Map<Integer, List<FileChunk>> byIndex = allChunks.stream()
                .collect(Collectors.groupingBy(FileChunk::getChunkIndex));

        List<Map<String, Object>> chunkList = new ArrayList<>();
        for (Map.Entry<Integer, List<FileChunk>> entry : new TreeMap<>(byIndex).entrySet()) {
            List<Map<String, Object>> replicas = entry.getValue().stream().map(c -> {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("nodeId", c.getNodeId());
                r.put("chunkId", c.getId());
                r.put("checksum", c.getChecksum());
                NodeStatus status = storageNodeRepository.findById(c.getNodeId())
                        .map(StorageNode::getStatus).orElse(NodeStatus.UNKNOWN);
                r.put("nodeStatus", status);
                return r;
            }).toList();

            Map<String, Object> chunkEntry = new LinkedHashMap<>();
            chunkEntry.put("chunkIndex", entry.getKey());
            chunkEntry.put("replicas", replicas);
            chunkList.add(chunkEntry);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("fileId", fileId);
        result.put("filename", metadata.getOriginalFilename());
        result.put("totalChunks", metadata.getChunkCount());
        result.put("replicationFactor", metadata.getReplicationFactor());
        result.put("chunks", chunkList);
        return result;
    }
}
