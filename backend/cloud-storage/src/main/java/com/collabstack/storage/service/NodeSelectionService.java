package com.collabstack.storage.service;

import com.collabstack.storage.entity.NodeStatus;
import com.collabstack.storage.entity.StorageNode;
import com.collabstack.storage.exception.StorageException;
import com.collabstack.storage.repository.StorageNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class NodeSelectionService {

    private final StorageNodeRepository storageNodeRepository;
    private final AtomicInteger roundRobinCounter = new AtomicInteger(0);

    /**
     * Returns {@code replicationFactor} distinct UP nodes using round-robin.
     * Throws {@link StorageException} if fewer nodes are available than required.
     */
    public List<StorageNode> selectNodesForChunk(int replicationFactor) {
        List<StorageNode> upNodes = storageNodeRepository.findAllByStatus(NodeStatus.UP);
        if (upNodes.size() < replicationFactor) {
            throw new StorageException(String.format(
                    "Not enough healthy nodes: need %d, found %d", replicationFactor, upNodes.size()));
        }

        List<StorageNode> selected = new ArrayList<>();
        int startIndex = roundRobinCounter.getAndIncrement() % upNodes.size();
        for (int i = 0; i < replicationFactor; i++) {
            selected.add(upNodes.get((startIndex + i) % upNodes.size()));
        }
        log.debug("Selected {} nodes for chunk placement: {}", replicationFactor,
                selected.stream().map(StorageNode::getId).toList());
        return selected;
    }
}
