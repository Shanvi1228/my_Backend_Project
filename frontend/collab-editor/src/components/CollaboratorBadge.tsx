import React from 'react';

interface CollaboratorBadgeProps {
  username: string;
}

const CollaboratorBadge: React.FC<CollaboratorBadgeProps> = ({ username }) => {
  // Generate a consistent color based on username
  const getColorFromUsername = (name: string): string => {
    let hash = 0;
    for (let i = 0; i < name.length; i++) {
      hash = name.charCodeAt(i) + ((hash << 5) - hash);
    }
    const colors = [
      'bg-red-500',
      'bg-blue-500',
      'bg-green-500',
      'bg-yellow-500',
      'bg-purple-500',
      'bg-pink-500',
      'bg-indigo-500',
      'bg-teal-500',
    ];
    return colors[Math.abs(hash) % colors.length];
  };

  const getInitials = (name: string): string => {
    return name
      .split(' ')
      .map((part) => part[0])
      .join('')
      .toUpperCase()
      .slice(0, 2);
  };

  const colorClass = getColorFromUsername(username);
  const initials = getInitials(username);

  return (
    <div className="flex items-center space-x-2">
      <div
        className={`w-8 h-8 rounded-full ${colorClass} flex items-center justify-center text-white text-xs font-semibold`}
        title={username}
      >
        {initials}
      </div>
      <span className="text-sm text-gray-700">{username}</span>
    </div>
  );
};

export default CollaboratorBadge;