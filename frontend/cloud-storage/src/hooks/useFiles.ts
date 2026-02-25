import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getFiles, deleteFile } from '../api/storage.api';

export const useFiles = () => {
  const queryClient = useQueryClient();

  const { data: files = [], isLoading, refetch } = useQuery({
    queryKey: ['files'],
    queryFn: getFiles,
  });

  const deleteMutation = useMutation({
    mutationFn: deleteFile,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['files'] });
    },
  });

  return {
    files,
    isLoading,
    refetch,
    deleteFile: (id: string) => deleteMutation.mutateAsync(id),
    isDeleting: deleteMutation.isPending,
  };
};