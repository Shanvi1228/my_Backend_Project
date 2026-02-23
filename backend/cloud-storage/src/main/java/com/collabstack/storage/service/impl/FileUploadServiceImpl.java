package com.collabstack.storage.service.impl;

import com.collabstack.storage.dto.response.UploadResponse;
import com.collabstack.storage.entity.*;
import com.collabstack.storage.exception.ResourceNotFoundException;
import com.collabstack.storage.exception.StorageException;
import com.collabstack.storage.repository.*;
import com.collabstack.storage.service.FileUploadService;
import com.collabstack.storage.service.NodeSelectionService;
import com.collabstack.storage.service.StorageNodeClient;
import com.collabstack.storage.util.CryptoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.SecretKey;
import java.security.MessageDigest;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileUploadServiceImpl implements FileUploadService {

    private static final int CHUNK_SIZE = 4 * 1024 * 1024; // 4 MB
    private static final int DEFAULT_REPLICATION_FACTOR = 3;

    private final UserRepository userRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final FileChunkRepository fileChunkRepository;
    private final EncryptedKeyRepository encryptedKeyRepository;
    private final StorageNodeRepository storageNodeRepository;
    private final NodeSelectionService nodeSelectionService;
    private final StorageNodeClient storageNodeClient;
    private final CryptoUtil cryptoUtil;

    @Override
    @Transactional
    public UploadResponse upload(UUID userId, MultipartFile file, String userPassword) {
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (Exception e) {
            throw new StorageException("Failed to read uploaded file: " + e.getMessage());
        }

        // 1. Generate random AES-256 DEK
        SecretKey dek;
        try {
            dek = cryptoUtil.generateDek();
        } catch (Exception e) {
            throw new StorageException("Failed to generate encryption key: " + e.getMessage());
        }

        // 2. Derive KEK from user password
        byte[] kekSalt = cryptoUtil.generateSalt();
        SecretKey kek;
        try {
            kek = cryptoUtil.deriveKek(userPassword, kekSalt);
        } catch (Exception e) {
            throw new StorageException("Failed to derive key from password: " + e.getMessage());
        }

        // 3. Encrypt DEK with KEK
        byte[] encryptedDekBytes;
        try {
            encryptedDekBytes = cryptoUtil.encryptKey(dek, kek);
        } catch (Exception e) {
            throw new StorageException("Failed to encrypt DEK: " + e.getMessage());
        }

        // 4. Split file bytes into 4 MB chunks
        int totalChunks = (int) Math.ceil((double) fileBytes.length / CHUNK_SIZE);
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload";
        String contentType = file.getContentType();

        // Save FileMetadata with UPLOADING status
        FileMetadata metadata = FileMetadata.builder()
                .owner(owner)
                .originalFilename(filename)
                .contentType(contentType)
                .totalSizeBytes(fileBytes.length)
                .chunkCount(totalChunks)
                .replicationFactor(DEFAULT_REPLICATION_FACTOR)
                .status(FileStatus.UPLOADING)
                .build();
        FileMetadata savedMetadata = fileMetadataRepository.save(metadata);

        // Save EncryptedKey
        EncryptedKey encryptedKey = EncryptedKey.builder()
                .file(savedMetadata)
                .encryptedDek(Base64.getEncoder().encodeToString(encryptedDekBytes))
                .kekSalt(Base64.getEncoder().encodeToString(kekSalt))
                .iv("")
                .build();
        encryptedKeyRepository.save(encryptedKey);

        // 5. Process each chunk
        for (int i = 0; i < totalChunks; i++) {
            int start = i * CHUNK_SIZE;
            int end = Math.min(start + CHUNK_SIZE, fileBytes.length);
            byte[] chunkData = Arrays.copyOfRange(fileBytes, start, end);

            // a. Encrypt chunk
            byte[] encryptedChunk;
            try {
                encryptedChunk = cryptoUtil.encrypt(chunkData, dek);
            } catch (Exception e) {
                throw new StorageException("Failed to encrypt chunk " + i + ": " + e.getMessage());
            }

            // b. Compute SHA-256 checksum of encrypted bytes
            String checksum = sha256Hex(encryptedChunk);

            // c. Select replicationFactor healthy nodes
            List<StorageNode> targetNodes;
            try {
                targetNodes = nodeSelectionService.selectNodesForChunk(DEFAULT_REPLICATION_FACTOR);
            } catch (StorageException e) {
                log.warn("Not enough nodes for replication (chunk {}): {}", i, e.getMessage());
                // Fallback: use however many UP nodes exist
                targetNodes = storageNodeRepository.findAllByStatus(NodeStatus.UP);
                if (targetNodes.isEmpty()) {
                    throw new StorageException("No storage nodes available for upload");
                }
            }

            // d. Upload to each node
            UUID chunkId = UUID.randomUUID();
            for (StorageNode node : targetNodes) {
                try {
                    storageNodeClient.uploadChunk(node.getId(), node.getHost(), node.getPort(), chunkId, encryptedChunk);
                    // e. Save FileChunk record — store chunkId as chunkStorageId so we can retrieve it later
                    FileChunk chunk = FileChunk.builder()
                            .file(savedMetadata)
                            .chunkIndex(i)
                            .nodeId(node.getId())
                            .chunkStorageId(chunkId)
                            .checksum(checksum)
                            .sizeBytes(encryptedChunk.length)
                            .build();
                    fileChunkRepository.save(chunk);
                } catch (StorageException ex) {
                    log.error("Failed to upload chunk {} to node {}: {}", i, node.getId(), ex.getMessage());
                }
            }
        }

        // 6. Mark file as COMPLETE
        savedMetadata.setStatus(FileStatus.COMPLETE);
        fileMetadataRepository.save(savedMetadata);

        log.info("File '{}' uploaded: {} chunks, {} replicas each", filename, totalChunks, DEFAULT_REPLICATION_FACTOR);
        return new UploadResponse(savedMetadata.getId(), filename, totalChunks, DEFAULT_REPLICATION_FACTOR);
    }

    private String sha256Hex(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
