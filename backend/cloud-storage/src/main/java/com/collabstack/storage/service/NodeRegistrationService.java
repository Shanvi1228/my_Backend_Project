package com.collabstack.storage.service;

import com.collabstack.storage.config.StorageNodeProperties;
import com.collabstack.storage.entity.NodeStatus;
import com.collabstack.storage.entity.StorageNode;
import com.collabstack.storage.repository.StorageNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class NodeRegistrationService {

    private final StorageNodeProperties storageNodeProperties;
    private final StorageNodeClient storageNodeClient;
    private final StorageNodeRepository storageNodeRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void registerNodesOnStartup() {
        log.info("Registering {} configured storage nodes...", storageNodeProperties.getNodes().size());
        for (StorageNodeProperties.NodeConfig cfg : storageNodeProperties.getNodes()) {
            StorageNode probe = StorageNode.builder()
                    .id(cfg.getId())
                    .host(cfg.getHost())
                    .port(cfg.getPort())
                    .status(NodeStatus.UNKNOWN)
                    .build();

            boolean healthy = storageNodeClient.isNodeHealthy(probe);
            NodeStatus status = healthy ? NodeStatus.UP : NodeStatus.UNKNOWN;

            StorageNode node = storageNodeRepository.findById(cfg.getId())
                    .orElse(StorageNode.builder()
                            .id(cfg.getId())
                            .host(cfg.getHost())
                            .port(cfg.getPort())
                            .build());

            node.setHost(cfg.getHost());
            node.setPort(cfg.getPort());
            node.setStatus(status);
            if (healthy) node.setLastHeartbeat(Instant.now());

            storageNodeRepository.save(node);
            log.info("Node [{}] at {}:{} — {}", cfg.getId(), cfg.getHost(), cfg.getPort(), status);
        }
    }
}
