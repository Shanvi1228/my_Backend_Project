package com.collabstack.storage.service;

import com.collabstack.storage.dto.request.RegisterNodeRequest;
import com.collabstack.storage.dto.response.StorageNodeResponse;

import java.util.List;

public interface StorageNodeService {
    List<StorageNodeResponse> listNodes();
    StorageNodeResponse registerNode(RegisterNodeRequest request);
}
