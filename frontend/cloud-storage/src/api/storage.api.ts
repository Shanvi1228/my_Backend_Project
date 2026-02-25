import client from './client';
import type { ApiResponse } from '../types/common.types';
import type {
  FileMetadataResponse,
  UploadResponse,
  StorageNodeResponse,
  FileChunkMap,
} from '../types/storage.types';

export const uploadFile = (
  file: File,
  password: string,
  replicationFactor: number = 3,
  onProgress?: (pct: number) => void
): Promise<UploadResponse> => {
  const formData = new FormData();
  formData.append('file', file);

  return client
    .post<ApiResponse<UploadResponse>>(
      `/files/upload?password=${encodeURIComponent(password)}&replicationFactor=${replicationFactor}`,
      formData,
      {
        headers: { 'Content-Type': 'multipart/form-data' },
        onUploadProgress: (progressEvent) => {
          if (onProgress && progressEvent.total) {
            const pct = Math.round((progressEvent.loaded * 100) / progressEvent.total);
            onProgress(pct);
          }
        },
      }
    )
    .then((r) => r.data.data);
};

export const getFiles = (): Promise<FileMetadataResponse[]> =>
  client.get<ApiResponse<FileMetadataResponse[]>>('/files').then((r) => r.data.data);

export const downloadFile = async (
  fileId: string,
  filename: string,
  password: string
): Promise<void> => {
  const response = await client.get(`/files/${fileId}?password=${encodeURIComponent(password)}`, {
    responseType: 'blob',
  });

  const url = window.URL.createObjectURL(new Blob([response.data]));
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', filename);
  document.body.appendChild(link);
  link.click();
  link.parentNode?.removeChild(link);
  window.URL.revokeObjectURL(url);
};

export const deleteFile = (fileId: string): Promise<void> =>
  client.delete(`/files/${fileId}`);

export const getNodes = (): Promise<StorageNodeResponse[]> =>
  client.get<ApiResponse<StorageNodeResponse[]>>('/admin/nodes').then((r) => r.data.data);

export const getFileChunkMap = (fileId: string): Promise<FileChunkMap> =>
  client.get<ApiResponse<FileChunkMap>>(`/admin/files/${fileId}/chunks`).then((r) => r.data.data);

export const triggerRepair = (): Promise<string> =>
  client.post<ApiResponse<string>>('/admin/repair').then((r) => r.data.message);