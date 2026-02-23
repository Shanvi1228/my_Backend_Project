package com.collabstack.editor.repository;

import com.collabstack.editor.entity.DocumentCollaborator;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentCollaboratorRepository extends JpaRepository<DocumentCollaborator, UUID> {
    Optional<DocumentCollaborator> findByDocument_IdAndUser_Id(UUID documentId, UUID userId);
    List<DocumentCollaborator> findAllByDocument_Id(UUID documentId);
    boolean existsByDocument_IdAndUser_Id(UUID documentId, UUID userId);
}
