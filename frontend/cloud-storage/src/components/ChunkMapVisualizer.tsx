import React from 'react';
import type { FileChunkMap, StorageNodeResponse } from '../types/storage.types';

interface ChunkMapVisualizerProps {
  chunkMap: FileChunkMap;
  nodes: StorageNodeResponse[];
}

const ChunkMapVisualizer: React.FC<ChunkMapVisualizerProps> = ({ chunkMap, nodes }) => {
  const hasReplica = (chunkIndex: number, nodeId: string): boolean => {
    const chunk = chunkMap.chunks.find((c) => c.chunkIndex === chunkIndex);
    return chunk ? chunk.replicas.some((r) => r.nodeId === nodeId) : false;
  };

  const getNodeStatus = (nodeId: string): string => {
    const node = nodes.find((n) => n.id === nodeId);
    return node?.status || 'UNKNOWN';
  };

  return (
    <div className="overflow-x-auto">
      <table className="min-w-full divide-y divide-gray-200 border border-gray-200">
        <thead className="bg-gray-50">
          <tr>
            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider border-r">
              Chunk
            </th>
            {nodes.map((node) => (
              <th
                key={node.id}
                className="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider"
              >
                {node.id}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="bg-white divide-y divide-gray-200">
          {chunkMap.chunks.map((chunk) => (
            <tr key={chunk.chunkIndex}>
              <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900 border-r">
                Chunk {chunk.chunkIndex}
              </td>
              {nodes.map((node) => {
                const has = hasReplica(chunk.chunkIndex, node.id);
                const status = getNodeStatus(node.id);

                let cellClass = 'px-6 py-4 whitespace-nowrap text-center';
                let content = '—';

                if (has) {
                  if (status === 'UP') {
                    cellClass += ' text-green-600 font-bold';
                    content = '✓';
                  } else if (status === 'DOWN') {
                    cellClass += ' text-red-600 font-bold';
                    content = '✗';
                  } else {
                    cellClass += ' text-gray-600';
                    content = '?';
                  }
                } else {
                  cellClass += ' text-gray-300';
                }

                return (
                  <td key={node.id} className={cellClass}>
                    <span className="text-lg">{content}</span>
                  </td>
                );
              })}
            </tr>
          ))}
        </tbody>
      </table>
      <div className="mt-4 text-sm text-gray-600 space-y-1">
        <p><span className="text-green-600 font-bold">✓</span> = Replica exists, node UP</p>
        <p><span className="text-red-600 font-bold">✗</span> = Replica exists, node DOWN</p>
        <p><span className="text-gray-300">—</span> = No replica on this node</p>
      </div>
    </div>
  );
};

export default ChunkMapVisualizer;