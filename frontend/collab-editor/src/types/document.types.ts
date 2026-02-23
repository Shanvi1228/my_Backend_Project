export interface DocumentResponse {
  id: string;
  title: string;
  contentSnapshot: string;
  currentRevision: number;
  ownerUsername: string;
  collaboratorCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface DocumentCreateRequest {
  title: string;
  initialContent?: string;
}

export interface CollaboratorResponse {
  userId: string;
  username: string;
  email: string;
  role: string;
}

export interface OperationMessage {
  type: 'OPERATION' | 'PRESENCE' | 'SYNC';
  opType?: 'INSERT' | 'DELETE';
  position?: number;
  content?: string;
  length?: number;
  clientRevision?: number;
  userId?: string;
  username?: string;
}

export interface ChatRequest {
  question: string;
}

export interface ChatResponse {
  answer: string;
  sourceSnippets: string[];
}