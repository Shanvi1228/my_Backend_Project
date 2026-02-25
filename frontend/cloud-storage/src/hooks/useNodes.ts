import { useQuery } from '@tanstack/react-query';
import { getNodes } from '../api/storage.api';

export const useNodes = () => {
  const { data: nodes = [], isLoading } = useQuery({
    queryKey: ['nodes'],
    queryFn: getNodes,
    refetchInterval: 10000, // Auto-poll every 10 seconds
  });

  return {
    nodes,
    isLoading,
  };
};