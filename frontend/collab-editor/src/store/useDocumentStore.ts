import { create } from 'zustand';

interface DocumentState {
  activeDocumentId: string | null;
  connectedUsers: string[];
  setActiveDocument: (id: string | null) => void;
  addConnectedUser: (username: string) => void;
  removeConnectedUser: (username: string) => void;
  setConnectedUsers: (users: string[]) => void;
}

export const useDocumentStore = create<DocumentState>((set) => ({
  activeDocumentId: null,
  connectedUsers: [],
  setActiveDocument: (id) => set({ activeDocumentId: id, connectedUsers: [] }),
  addConnectedUser: (username) => set((state) => ({
    connectedUsers: state.connectedUsers.includes(username)
      ? state.connectedUsers
      : [...state.connectedUsers, username],
  })),
  removeConnectedUser: (username) => set((state) => ({
    connectedUsers: state.connectedUsers.filter((u) => u !== username),
  })),
  setConnectedUsers: (users) => set({ connectedUsers: users }),
}));