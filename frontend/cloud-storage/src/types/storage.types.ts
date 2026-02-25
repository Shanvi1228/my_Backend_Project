export interface FileMetadataResponse {
  id: string;
  filename: string;
  contentType: string;
  sizeBytes: number;
  chunkCount: number;
  status: 'UPLOADING' | 'COMPLETE' | 'DEGRADED';
  createdAt: string;
}

export interface StorageNodeResponse {
  id: string;
  host: string;
  port: number;
  status: 'UP' | 'DOWN' | 'UNKNOWN';
}

export interface UploadResponse {
  fileId: string;
  filename: string;
  chunkCount: number;
  replicasPerChunk: number;
}

export interface ChunkReplica {
  nodeId: string;
  status: string;
  checksum: string;
}

export interface ChunkMap {
  chunkIndex: number;
  replicas: ChunkReplica[];
}

export interface FileChunkMap {
  fileId: string;
  filename?: string;
  chunks: ChunkMap[];
}