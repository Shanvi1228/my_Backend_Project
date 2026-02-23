import React from 'react';
import CollaboratorBadge from './CollaboratorBadge';

interface PresenceBarProps {
  connectedUsers: string[];
}

const PresenceBar: React.FC<PresenceBarProps> = ({ connectedUsers }) => {
  if (connectedUsers.length === 0) {
    return null;
  }

  return (
    <div className="bg-blue-50 border-b border-blue-200 px-4 py-2">
      <div className="flex items-center space-x-4">
        <span className="text-sm text-gray-700 font-medium">
          {connectedUsers.length} {connectedUsers.length === 1 ? 'person' : 'people'} editing
        </span>
        <div className="flex items-center space-x-3">
          {connectedUsers.map((username) => (
            <CollaboratorBadge key={username} username={username} />
          ))}
        </div>
      </div>
    </div>
  );
};

export default PresenceBar;