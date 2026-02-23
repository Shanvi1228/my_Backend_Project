package com.collabstack.editor.repository;

import com.collabstack.editor.entity.DocumentOperation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DocumentOperationRepository extends JpaRepository<DocumentOperation, UUID> {
}
