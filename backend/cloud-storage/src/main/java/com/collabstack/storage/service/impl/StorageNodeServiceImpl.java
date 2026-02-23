package com.collabstack.storage.service.impl;

import com.collabstack.storage.dto.request.RegisterNodeRequest;
import com.collabstack.storage.dto.response.StorageNodeResponse;
import com.collabstack.storage.entity.NodeStatus;
import com.collabstack.storage.entity.StorageNode;
import com.collabstack.storage.repository.StorageNodeRepository;
import com.collabstack.storage.service.StorageNodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StorageNodeServiceImpl implements StorageNodeService {

    private final StorageNodeRepository storageNodeRepository;

    @Override
    @Transactional(readOnly = true)
    public List<StorageNodeResponse> listNodes() {
        return storageNodeRepository.findAll().stream()
                .map(n -> new StorageNodeResponse(n.getId(), n.getHost(), n.getPort(), n.getStatus()))
                .toList();
    }

    @Override
    @Transactional
    public StorageNodeResponse registerNode(RegisterNodeRequest request) {
        StorageNode node = StorageNode.builder()
                .id(request.id())
                .host(request.host())
                .port(request.port())
                .status(NodeStatus.UP)
                .lastHeartbeat(Instant.now())
                .build();
        StorageNode saved = storageNodeRepository.save(node);
        log.info("Registered storage node: {}", saved.getId());
        return new StorageNodeResponse(saved.getId(), saved.getHost(), saved.getPort(), saved.getStatus());
    }
}
