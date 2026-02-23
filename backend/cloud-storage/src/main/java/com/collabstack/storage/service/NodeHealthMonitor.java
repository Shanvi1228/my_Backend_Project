package com.collabstack.storage.service;

import com.collabstack.storage.entity.NodeStatus;
import com.collabstack.storage.entity.StorageNode;
import com.collabstack.storage.repository.StorageNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class NodeHealthMonitor {

    private final StorageNodeRepository storageNodeRepository;
    private final StorageNodeClient storageNodeClient;

    @Scheduled(fixedDelayString = "${storage.replication.health-check-interval-ms:10000}")
    @Transactional
    public void checkAllNodes() {
        List<StorageNode> allNodes = storageNodeRepository.findAll();
        if (allNodes.isEmpty()) return;

        for (StorageNode node : allNodes) {
            boolean healthy = storageNodeClient.isNodeHealthy(node);
            NodeStatus newStatus = healthy ? NodeStatus.UP : NodeStatus.DOWN;

            if (node.getStatus() != newStatus) {
                log.warn("Node {} status changed: {} → {}", node.getId(), node.getStatus(), newStatus);
            }

            node.setStatus(newStatus);
            if (healthy) node.setLastHeartbeat(Instant.now());
            storageNodeRepository.save(node);
        }
    }
}
