import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNodes } from '../hooks/useNodes';
import { useFiles } from '../hooks/useFiles';
import { getFileChunkMap, triggerRepair } from '../api/storage.api';
import NodeStatusBadge from '../components/NodeStatusBadge';
import ChunkMapVisualizer from '../components/ChunkMapVisualizer';

const AdminPage: React.FC = () => {
  const { nodes, isLoading: nodesLoading } = useNodes();
  const { files, isLoading: filesLoading } = useFiles();
  const [selectedFileId, setSelectedFileId] = useState<string>('');
  const [showToast, setShowToast] = useState(false);

  const { data: chunkMap, isLoading: mapLoading } = useQuery({
    queryKey: ['chunkMap', selectedFileId],
    queryFn: () => getFileChunkMap(selectedFileId),
    enabled: !!selectedFileId,
  });

  const handleTriggerRepair = async () => {
    try {
      await triggerRepair();
      setShowToast(true);
      setTimeout(() => setShowToast(false), 3000);
    } catch (error) {
      alert('Failed to trigger repair');
    }
  };

  return (
    <div className="px-4 py-6">
      <h1 className="text-3xl font-bold text-gray-900 mb-8">Admin Panel</h1>

      {/* Node Health Section */}
      <div className="bg-white rounded-lg border border-gray-200 p-6 mb-6">
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-xl font-semibold">Storage Node Health</h2>
          <button
            onClick={handleTriggerRepair}
            className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 text-sm font-medium"
          >
            Trigger Repair
          </button>
        </div>

        {nodesLoading ? (
          <div className="text-gray-500">Loading nodes...</div>
        ) : nodes.length === 0 ? (
          <div className="text-gray-500">No storage nodes found</div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            {nodes.map((node) => (
              <div key={node.id} className="bg-gray-50 rounded-lg p-4">
                <NodeStatusBadge node={node} />
                <p className="text-xs text-gray-500 mt-2">
                  {node.host}:{node.port}
                </p>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* File Selector Section */}
      <div className="bg-white rounded-lg border border-gray-200 p-6 mb-6">
        <h2 className="text-xl font-semibold mb-4">Chunk Distribution Viewer</h2>
        
        {filesLoading ? (
          <div className="text-gray-500">Loading files...</div>
        ) : files.length === 0 ? (
          <div className="text-gray-500">No files to display</div>
        ) : (
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Select a file to view its chunk distribution:
            </label>
            <select
              value={selectedFileId}
              onChange={(e) => setSelectedFileId(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-purple-500"
            >
              <option value="">-- Choose a file --</option>
              {files.map((file) => (
                <option key={file.id} value={file.id}>
                  {file.filename} ({file.chunkCount} chunks, {file.status})
                </option>
              ))}
            </select>
          </div>
        )}
      </div>

      {/* Chunk Map Visualization */}
      {selectedFileId && (
        <div className="bg-white rounded-lg border border-gray-200 p-6">
          <h2 className="text-xl font-semibold mb-4">
            Replica Distribution
          </h2>
          
          {mapLoading ? (
            <div className="text-gray-500">Loading chunk map...</div>
          ) : chunkMap ? (
            <div>
              <p className="text-sm text-gray-600 mb-4">
                Showing replica distribution for: <strong>{chunkMap.filename || 'Selected file'}</strong>
              </p>
              <ChunkMapVisualizer chunkMap={chunkMap} nodes={nodes} />
            </div>
          ) : (
            <div className="text-red-500">Failed to load chunk map</div>
          )}
        </div>
      )}

      {/* Toast Notification */}
      {showToast && (
        <div className="fixed bottom-4 right-4 bg-green-600 text-white px-6 py-3 rounded-lg shadow-lg">
          Repair job triggered successfully!
        </div>
      )}
    </div>
  );
};

export default AdminPage;