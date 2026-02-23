package com.collabstack.editor.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "document_operations")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "op_type", nullable = false, length = 20)
    private String opType;

    @Column(name = "position", nullable = false)
    private Integer position;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "length")
    private Integer length;

    @Column(name = "revision", nullable = false)
    private Long revision;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
