package com.collabstack.storage.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "storage_nodes")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageNode {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(nullable = false)
    private String host;

    @Column(nullable = false)
    private int port;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private NodeStatus status = NodeStatus.UNKNOWN;

    @Column(name = "last_heartbeat")
    private Instant lastHeartbeat;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
