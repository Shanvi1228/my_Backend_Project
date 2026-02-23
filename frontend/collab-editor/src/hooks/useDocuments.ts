import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getDocuments, createDocument, deleteDocument } from '../api/documents.api';
import type { DocumentCreateRequest } from '../types/document.types';

export const useDocuments = () => {
  const queryClient = useQueryClient();

  const { data: documents = [], isLoading } = useQuery({
    queryKey: ['documents'],
    queryFn: getDocuments,
  });

  const createMutation = useMutation({
    mutationFn: createDocument,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['documents'] });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: deleteDocument,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['documents'] });
    },
  });

  return {
    documents,
    isLoading,
    createDocument: (req: DocumentCreateRequest) => createMutation.mutateAsync(req),
    deleteDocument: (id: string) => deleteMutation.mutateAsync(id),
    isCreating: createMutation.isPending,
    isDeleting: deleteMutation.isPending,
  };
};