package com.collabstack.storage.repository;

import com.collabstack.storage.entity.EncryptedKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EncryptedKeyRepository extends JpaRepository<EncryptedKey, UUID> {
    Optional<EncryptedKey> findByFile_Id(UUID fileId);
    void deleteByFile_Id(UUID fileId);
}
