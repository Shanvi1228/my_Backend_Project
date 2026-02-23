package com.collabstack.storage.dto.response;

import com.collabstack.storage.entity.NodeStatus;

public record StorageNodeResponse(
        String id,
        String host,
        int port,
        NodeStatus status
) {}
