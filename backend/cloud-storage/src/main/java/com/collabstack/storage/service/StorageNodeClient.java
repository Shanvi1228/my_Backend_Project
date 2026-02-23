package com.collabstack.storage.service;

import com.collabstack.storage.entity.StorageNode;
import com.collabstack.storage.exception.StorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class StorageNodeClient {

    private final RestTemplate restTemplate;

    /**
     * Uploads an encrypted chunk to a storage node via PUT /chunks/{chunkId}
     */
    public void uploadChunk(String nodeId, String host, int port, UUID chunkId, byte[] encryptedData) {
        String url = String.format("http://%s:%d/chunks/%s", host, port, chunkId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        HttpEntity<byte[]> entity = new HttpEntity<>(encryptedData, headers);
        try {
            restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);
            log.debug("Uploaded chunk {} to node {}", chunkId, nodeId);
        } catch (RestClientException e) {
            throw new StorageException("Failed to upload chunk " + chunkId + " to node " + nodeId + ": " + e.getMessage());
        }
    }

    /**
     * Downloads an encrypted chunk from a storage node via GET /chunks/{chunkId}
     */
    public byte[] downloadChunk(String nodeId, String host, int port, UUID chunkId) {
        String url = String.format("http://%s:%d/chunks/%s", host, port, chunkId);
        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    url, HttpMethod.GET, HttpEntity.EMPTY, byte[].class);
            if (response.getBody() == null) {
                throw new StorageException("Node " + nodeId + " returned empty body for chunk " + chunkId);
            }
            log.debug("Downloaded chunk {} from node {}", chunkId, nodeId);
            return response.getBody();
        } catch (RestClientException e) {
            throw new StorageException("Failed to download chunk " + chunkId + " from node " + nodeId + ": " + e.getMessage());
        }
    }

    /**
     * Deletes a chunk from a storage node via DELETE /chunks/{chunkId}
     */
    public void deleteChunk(String nodeId, String host, int port, UUID chunkId) {
        String url = String.format("http://%s:%d/chunks/%s", host, port, chunkId);
        try {
            restTemplate.delete(url);
            log.debug("Deleted chunk {} from node {}", chunkId, nodeId);
        } catch (RestClientException e) {
            log.warn("Failed to delete chunk {} from node {}: {}", chunkId, nodeId, e.getMessage());
        }
    }

    /**
     * Checks node health via GET /health
     */
    public boolean isNodeHealthy(StorageNode node) {
        String url = String.format("http://%s:%d/chunks/health", node.getHost(), node.getPort());
        try {
            ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.GET, HttpEntity.EMPTY, Void.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (RestClientException e) {
            log.warn("Node {} health check failed: {}", node.getId(), e.getMessage());
            return false;
        }
    }
}
