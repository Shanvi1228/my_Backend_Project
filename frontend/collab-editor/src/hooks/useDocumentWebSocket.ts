import { useState, useEffect, useRef, useCallback } from 'react';
import type { OperationMessage } from '../types/document.types';
import { useAuthStore } from '../store/useAuthStore';
import { useDocumentStore } from '../store/useDocumentStore';

interface UseDocumentWebSocketReturn {
  isConnected: boolean;
  sendOperation: (op: OperationMessage) => void;
}

export const useDocumentWebSocket = (
  documentId: string,
  onRemoteOperation: (op: OperationMessage) => void
): UseDocumentWebSocketReturn => {
  const [isConnected, setIsConnected] = useState(false);
  const socketRef = useRef<WebSocket | null>(null);
  const token = useAuthStore((s) => s.token);
  const { addConnectedUser, removeConnectedUser, setConnectedUsers } = useDocumentStore();

  useEffect(() => {
    if (!token || !documentId) return;

    const wsUrl = `${import.meta.env.VITE_WS_BASE_URL}/ws/documents/${documentId}?token=${token}`;
    const ws = new WebSocket(wsUrl);
    socketRef.current = ws;

    ws.onopen = () => {
      console.log('WebSocket connected');
      setIsConnected(true);
    };

    ws.onmessage = (event) => {
      try {
        const message: OperationMessage = JSON.parse(event.data);
        
        if (message.type === 'PRESENCE') {
          if (message.username) {
            addConnectedUser(message.username);
          }
        } else if (message.type === 'SYNC') {
          // Sync message might contain list of connected users
          // For now, just acknowledge
        } else if (message.type === 'OPERATION') {
          onRemoteOperation(message);
        }
      } catch (error) {
        console.error('Error parsing WebSocket message:', error);
      }
    };

    ws.onclose = () => {
      console.log('WebSocket disconnected');
      setIsConnected(false);
    };

    ws.onerror = (error) => {
      console.error('WebSocket error:', error);
    };

    return () => {
      ws.close();
      setConnectedUsers([]);
    };
  }, [documentId, token, onRemoteOperation, addConnectedUser, setConnectedUsers]);

  const sendOperation = useCallback((op: OperationMessage) => {
    if (socketRef.current?.readyState === WebSocket.OPEN) {
      socketRef.current.send(JSON.stringify(op));
    }
  }, []);

  return { isConnected, sendOperation };
};