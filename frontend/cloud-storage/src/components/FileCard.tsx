import React, { useState } from 'react';
import { downloadFile } from '../api/storage.api';
import type { FileMetadataResponse } from '../types/storage.types';

interface FileCardProps {
  file: FileMetadataResponse;
  onDelete: (id: string) => void;
}

const FileCard: React.FC<FileCardProps> = ({ file, onDelete }) => {
  const [showPasswordDialog, setShowPasswordDialog] = useState(false);
  const [showDeleteDialog, setShowDeleteDialog] = useState(false);
  const [password, setPassword] = useState('');
  const [isDownloading, setIsDownloading] = useState(false);

  const formatSize = (bytes: number): string => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(2)} KB`;
    if (bytes < 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
    return `${(bytes / (1024 * 1024 * 1024)).toFixed(2)} GB`;
  };

  const getStatusBadge = () => {
    switch (file.status) {
      case 'COMPLETE':
        return <span className="px-2 py-1 text-xs bg-green-100 text-green-800 rounded">Complete</span>;
      case 'DEGRADED':
        return <span className="px-2 py-1 text-xs bg-red-100 text-red-800 rounded">Degraded</span>;
      case 'UPLOADING':
        return <span className="px-2 py-1 text-xs bg-yellow-100 text-yellow-800 rounded">Uploading</span>;
    }
  };

  const handleDownload = async () => {
    if (!password) return;
    setIsDownloading(true);
    try {
      await downloadFile(file.id, file.filename, password);
      setShowPasswordDialog(false);
      setPassword('');
    } catch (error) {
      alert('Download failed. Check your password and try again.');
    } finally {
      setIsDownloading(false);
    }
  };

  const handleDelete = () => {
    onDelete(file.id);
    setShowDeleteDialog(false);
  };

  return (
    <>
      <div className="bg-white rounded-lg border border-gray-200 p-6 hover:shadow-lg transition-shadow">
        <div className="flex justify-between items-start mb-4">
          <div className="flex-1 min-w-0">
            <h3 className="text-lg font-semibold text-gray-900 truncate">{file.filename}</h3>
            <p className="text-sm text-gray-500 mt-1">{formatSize(file.sizeBytes)}</p>
          </div>
          {getStatusBadge()}
        </div>
        <div className="space-y-2 text-sm text-gray-600 mb-4">
          <p>Chunks: {file.chunkCount}</p>
          <p>Uploaded: {new Date(file.createdAt).toLocaleDateString()}</p>
        </div>
        <div className="flex space-x-2">
          <button
            onClick={() => setShowPasswordDialog(true)}
            className="flex-1 px-4 py-2 bg-purple-600 text-white rounded-md hover:bg-purple-700 text-sm font-medium"
          >
            Download
          </button>
          <button
            onClick={() => setShowDeleteDialog(true)}
            className="px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700 text-sm font-medium"
          >
            Delete
          </button>
        </div>
      </div>

      {showPasswordDialog && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 max-w-md w-full mx-4">
            <h3 className="text-lg font-semibold mb-4">Enter Password</h3>
            <p className="text-sm text-gray-600 mb-4">
              Enter the password you used to encrypt this file.
            </p>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Password"
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-purple-500 mb-4"
              autoFocus
            />
            <div className="flex space-x-3 justify-end">
              <button
                onClick={() => {
                  setShowPasswordDialog(false);
                  setPassword('');
                }}
                className="px-4 py-2 bg-gray-200 text-gray-800 rounded-md hover:bg-gray-300"
                disabled={isDownloading}
              >
                Cancel
              </button>
              <button
                onClick={handleDownload}
                className="px-4 py-2 bg-purple-600 text-white rounded-md hover:bg-purple-700 disabled:bg-gray-400"
                disabled={isDownloading || !password}
              >
                {isDownloading ? 'Downloading...' : 'Download'}
              </button>
            </div>
          </div>
        </div>
      )}

      {showDeleteDialog && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 max-w-md w-full mx-4">
            <h3 className="text-lg font-semibold mb-4">Delete File?</h3>
            <p className="text-gray-600 mb-6">
              Are you sure you want to delete "{file.filename}"? This action cannot be undone.
            </p>
            <div className="flex space-x-3 justify-end">
              <button
                onClick={() => setShowDeleteDialog(false)}
                className="px-4 py-2 bg-gray-200 text-gray-800 rounded-md hover:bg-gray-300"
              >
                Cancel
              </button>
              <button
                onClick={handleDelete}
                className="px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700"
              >
                Delete
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
};

export default FileCard;