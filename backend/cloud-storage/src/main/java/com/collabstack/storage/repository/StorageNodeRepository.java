package com.collabstack.storage.repository;

import com.collabstack.storage.entity.NodeStatus;
import com.collabstack.storage.entity.StorageNode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StorageNodeRepository extends JpaRepository<StorageNode, String> {
    List<StorageNode> findAllByStatus(NodeStatus status);
}
