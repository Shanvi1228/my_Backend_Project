---
agent: agent
description: "Phase 10 – React frontend for the distributed cloud storage system"
tools: [codebase, editFiles]
---

Reference [copilot-instructions.md](../copilot-instructions.md). Phases 06–08 must be complete.

## Goal
Build `frontend/cloud-storage`. Clean dashboard-style UI with file management and an admin panel showing node health + chunk distribution.

## All Files to Create

### Types
`src/types/auth.types.ts` — identical to collab-editor (copy and reuse)

`src/types/storage.types.ts`:
```ts
export interface FileMetadataResponse {
  id: string; filename: string; contentType: string;
  sizeBytes: number; chunkCount: number;
  status: 'UPLOADING' | 'COMPLETE' | 'DEGRADED'; createdAt: string;
}
export interface StorageNodeResponse { id: string; host: string; port: number; status: 'UP' | 'DOWN' | 'UNKNOWN'; }
export interface UploadResponse { fileId: string; filename: string; chunkCount: number; replicasPerChunk: number; }
export interface ChunkReplica { nodeId: string; status: string; checksum: string; }
export interface ChunkMap { chunkIndex: number; replicas: ChunkReplica[]; }
export interface FileChunkMap { fileId: string; chunks: ChunkMap[]; }
```

### API Layer
`src/api/client.ts` — same Axios + JWT interceptor (points to port 8081)
`src/api/auth.api.ts` — identical to collab-editor
`src/api/storage.api.ts`:
```ts
export const uploadFile = (file: File, password: string, onProgress?: (pct: number) => void): Promise<UploadResponse>
  // Use FormData, axios onUploadProgress callback for progress reporting

export const getFiles = (): Promise<FileMetadataResponse[]>

export const downloadFile = async (fileId: string, filename: string, password: string): Promise<void>
  // Use axios with responseType: 'blob', create object URL, trigger download via <a> click

export const deleteFile = (fileId: string): Promise<void>

export const getNodes = (): Promise<StorageNodeResponse[]>

export const getFileChunkMap = (fileId: string): Promise<FileChunkMap>

export const triggerRepair = (): Promise<string>
```

### Store
`src/store/useAuthStore.ts` — same as collab-editor
`src/store/useStorageStore.ts`:
```ts
interface StorageState {
  uploadProgress: Record<string, number>;  // fileId/name → 0-100
  setProgress: (key: string, pct: number) => void;
  clearProgress: (key: string) => void;
}
```

### Hooks
`src/hooks/useFiles.ts`:
- TanStack Query for `getFiles()`, returns `{ files, isLoading, refetch }`
- `useDeleteFile` mutation — invalidates file list query on success

`src/hooks/useNodes.ts`:
- TanStack Query for `getNodes()` with `refetchInterval: 10000` (auto-poll every 10s)
- Returns `{ nodes, isLoading }`

### Components
`src/components/Layout.tsx`:
- Navbar: logo "CollabVault", links to `/files` and `/admin`, logout

`src/components/ProtectedRoute.tsx` — same pattern

`src/components/FileCard.tsx`:
- Shows: filename, size (formatted: KB/MB/GB), status badge (green=COMPLETE, red=DEGRADED, yellow=UPLOADING)
- Action buttons: Download (opens password dialog), Delete (with confirm)
- On Download: prompt for password → call `downloadFile()`

`src/components/UploadZone.tsx`:
- Drag-and-drop zone using HTML5 drag events (no external library)
- Shows file preview after selection: name + size
- Password input field (required for encryption)
- Upload button with progress bar (uses `uploadProgress` from store)
- On complete: shows success toast, refetches file list

`src/components/NodeStatusBadge.tsx`:
- Green dot = UP, red dot = DOWN, grey dot = UNKNOWN
- Shows `{nodeId}: UP` or `{nodeId}: DOWN` with dot indicator

`src/components/ChunkMapVisualizer.tsx`:
```tsx
// Props: fileChunkMap: FileChunkMap, nodes: StorageNodeResponse[]
// Renders a table:
// Rows = chunks (Chunk 0, Chunk 1, ...)
// Columns = nodes (Node-1, Node-2, Node-3)
// Cell = ✓ (green, healthy replica) | ✗ (red, node DOWN) | — (no replica on this node)
// This visualization is the "wow factor" of the admin panel
```

### Pages
`src/pages/LoginPage.tsx` — same as collab-editor
`src/pages/RegisterPage.tsx` — same as collab-editor

`src/pages/FilesPage.tsx`:
- Header: "My Files" + storage summary (`{n} files, {total size}`)
- `<UploadZone />` at top
- Grid/list of `<FileCard />` components
- Empty state when no files

`src/pages/AdminPage.tsx`:
```tsx
// Three sections:
// 1. Node Health Panel
//    - Row of NodeStatusBadge for each node
//    - Last heartbeat timestamp
//    - "Trigger Repair" button → calls triggerRepair() + success toast

// 2. File Selector
//    - Dropdown to select a file from the user's files
//    - On select: fetch FileChunkMap for that file

// 3. Chunk Distribution Map
//    - Shows <ChunkMapVisualizer /> for selected file
//    - Caption: "Showing replica distribution for: {filename}"
```

### App Router
`src/App.tsx`:
```tsx
<Routes>
  <Route path="/login" element={<LoginPage />} />
  <Route path="/register" element={<RegisterPage />} />
  <Route element={<ProtectedRoute><Layout /></ProtectedRoute>}>
    <Route path="/files" element={<FilesPage />} />
    <Route path="/admin" element={<AdminPage />} />
    <Route path="/" element={<Navigate to="/files" />} />
  </Route>
</Routes>
```

### Environment
`frontend/cloud-storage/.env.local`:
```
VITE_API_BASE_URL=http://localhost:8081/api
VITE_WS_BASE_URL=ws://localhost:8081
```

## Done When
- [ ] Login/Register works against cloud-storage service (port 8081)
- [ ] Drag-and-drop upload works with progress bar
- [ ] Files list shows all uploaded files with correct status badge
- [ ] Download works — original file is downloaded correctly with correct filename
- [ ] Admin panel shows live node status (auto-polls every 10s)
- [ ] Admin panel shows chunk distribution table after selecting a file
- [ ] Stopping a Docker node → admin panel shows it as DOWN within 15s
- [ ] "Trigger Repair" button works and shows toast confirmation
- [ ] DEGRADED file status (all replicas lost) shows red badge with clear message