import React from 'react';
import type { StorageNodeResponse } from '../types/storage.types';

interface NodeStatusBadgeProps {
  node: StorageNodeResponse;
}

const NodeStatusBadge: React.FC<NodeStatusBadgeProps> = ({ node }) => {
  const getStatusColor = () => {
    switch (node.status) {
      case 'UP':
        return 'bg-green-500';
      case 'DOWN':
        return 'bg-red-500';
      default:
        return 'bg-gray-400';
    }
  };

  const getTextColor = () => {
    switch (node.status) {
      case 'UP':
        return 'text-green-700';
      case 'DOWN':
        return 'text-red-700';
      default:
        return 'text-gray-700';
    }
  };

  return (
    <div className="flex items-center space-x-2">
      <div className={`w-3 h-3 rounded-full ${getStatusColor()}`}></div>
      <span className={`text-sm font-medium ${getTextColor()}`}>
        {node.id}: {node.status}
      </span>
    </div>
  );
};

export default NodeStatusBadge;