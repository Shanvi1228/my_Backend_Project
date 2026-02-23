package com.collabstack.storage.repository;

import com.collabstack.storage.entity.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FileMetadataRepository extends JpaRepository<FileMetadata, UUID> {
    List<FileMetadata> findAllByOwnerId(UUID ownerId);
    boolean existsByIdAndOwner_Id(UUID fileId, UUID ownerId);
}
