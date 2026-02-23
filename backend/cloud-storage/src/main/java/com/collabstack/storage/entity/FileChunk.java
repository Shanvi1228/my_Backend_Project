package com.collabstack.storage.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "file_chunks",
       uniqueConstraints = @UniqueConstraint(columnNames = {"file_id", "chunk_index", "node_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "file_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private FileMetadata file;

    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;

    @Column(name = "node_id", nullable = false, length = 50)
    private String nodeId;

    // UUID used as the filename on the storage node disk: {chunkStorageId}.enc
    @Column(name = "chunk_storage_id", nullable = false)
    private UUID chunkStorageId;

    @Column(name = "checksum", length = 64)
    private String checksum;

    @Column(name = "size_bytes", nullable = false)
    private int sizeBytes;
}
