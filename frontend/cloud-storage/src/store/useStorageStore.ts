import { create } from 'zustand';

interface StorageState {
  uploadProgress: Record<string, number>;
  setProgress: (key: string, pct: number) => void;
  clearProgress: (key: string) => void;
}

export const useStorageStore = create<StorageState>((set) => ({
  uploadProgress: {},
  setProgress: (key, pct) =>
    set((state) => ({
      uploadProgress: { ...state.uploadProgress, [key]: pct },
    })),
  clearProgress: (key) =>
    set((state) => {
      const { [key]: _, ...rest } = state.uploadProgress;
      return { uploadProgress: rest };
    }),
}));