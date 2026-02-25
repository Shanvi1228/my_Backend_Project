import React from 'react';
import { useFiles } from '../hooks/useFiles';
import UploadZone from '../components/UploadZone';
import FileCard from '../components/FileCard';

const FilesPage: React.FC = () => {
  const { files, isLoading, refetch, deleteFile } = useFiles();

  const totalSize = files.reduce((sum, file) => sum + file.sizeBytes, 0);

  const formatSize = (bytes: number): string => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(2)} KB`;
    if (bytes < 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
    return `${(bytes / (1024 * 1024 * 1024)).toFixed(2)} GB`;
  };

  const handleDelete = async (id: string) => {
    try {
      await deleteFile(id);
    } catch (error) {
      alert('Failed to delete file');
    }
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-gray-500">Loading files...</div>
      </div>
    );
  }

  return (
    <div className="px-4 py-6">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">My Files</h1>
        <p className="text-gray-600">
          {files.length} {files.length === 1 ? 'file' : 'files'} · {formatSize(totalSize)} total
        </p>
      </div>

      <UploadZone onUploadComplete={refetch} />

      {files.length === 0 ? (
        <div className="text-center py-12 bg-white rounded-lg border border-gray-200">
          <svg
            className="mx-auto h-12 w-12 text-gray-400"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"
            />
          </svg>
          <h3 className="text-lg font-medium text-gray-900 mt-4 mb-2">No files yet</h3>
          <p className="text-gray-500">Upload your first file using the form above</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {files.map((file) => (
            <FileCard key={file.id} file={file} onDelete={handleDelete} />
          ))}
        </div>
      )}
    </div>
  );
};

export default FilesPage;