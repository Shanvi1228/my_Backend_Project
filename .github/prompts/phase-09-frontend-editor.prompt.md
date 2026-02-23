---
agent: agent
description: "Phase 09 – React frontend for the collaborative editor with RAG chat sidebar"
tools: [codebase, editFiles]
---

Reference [copilot-instructions.md](../copilot-instructions.md). Phases 01–05 must be complete.

## Goal
Build the complete React UI for `frontend/collab-editor`. Clean, functional, minimal design using shadcn/ui + Tailwind.

## All Files to Create

### Types
`src/types/auth.types.ts`:
```ts
export interface UserInfo { id: string; email: string; username: string; }
export interface AuthResponse { token: string; userId: string; username: string; email: string; }
export interface LoginRequest { email: string; password: string; }
export interface RegisterRequest { email: string; username: string; password: string; }
```

`src/types/document.types.ts`:
```ts
export interface DocumentResponse {
  id: string; title: string; contentSnapshot: string;
  currentRevision: number; ownerUsername: string;
  collaboratorCount: number; createdAt: string; updatedAt: string;
}
export interface DocumentCreateRequest { title: string; initialContent?: string; }
export interface CollaboratorResponse { userId: string; username: string; email: string; role: string; }
export interface OperationMessage {
  type: 'OPERATION' | 'PRESENCE' | 'SYNC';
  opType?: 'INSERT' | 'DELETE';
  position?: number; content?: string; length?: number;
  clientRevision?: number; userId?: string; username?: string;
}
export interface ChatRequest { question: string; }
export interface ChatResponse { answer: string; sourceSnippets: string[]; }
```

### API Layer
`src/api/client.ts` — Axios instance with JWT interceptor (from instructions)

`src/api/auth.api.ts`:
```ts
export const login = (req: LoginRequest): Promise<AuthResponse> =>
  client.post<ApiResponse<AuthResponse>>('/auth/login', req).then(r => r.data.data);
export const register = (req: RegisterRequest): Promise<AuthResponse> =>
  client.post<ApiResponse<AuthResponse>>('/auth/register', req).then(r => r.data.data);
```

`src/api/documents.api.ts`:
```ts
export const getDocuments = (): Promise<DocumentResponse[]> => ...
export const createDocument = (req: DocumentCreateRequest): Promise<DocumentResponse> => ...
export const getDocument = (id: string): Promise<DocumentResponse> => ...
export const updateTitle = (id: string, title: string): Promise<DocumentResponse> => ...
export const deleteDocument = (id: string): Promise<void> => ...
export const addCollaborator = (id: string, email: string, role: string): Promise<CollaboratorResponse> => ...
export const chatWithDocument = (id: string, req: ChatRequest): Promise<ChatResponse> => ...
```

### Store
`src/store/useAuthStore.ts` — from instructions template
`src/store/useDocumentStore.ts`:
```ts
interface DocumentState {
  activeDocumentId: string | null;
  connectedUsers: string[];          // usernames currently in this doc
  setActiveDocument: (id: string | null) => void;
  addConnectedUser: (username: string) => void;
  removeConnectedUser: (username: string) => void;
}
```

### Custom Hooks
`src/hooks/useDocumentWebSocket.ts`:
- Connects to `VITE_WS_BASE_URL/ws/documents/{documentId}?token={jwt}`
- On message: parses JSON into `OperationMessage`
- Returns: `{ isConnected, connectedUsers, onRemoteOperation, sendOperation }`
- `onRemoteOperation`: a callback the editor page registers to apply incoming ops

`src/hooks/useDocuments.ts`:
- Wraps TanStack Query calls for document list, create, delete
- Returns `{ documents, isLoading, createDocument, deleteDocument }`

### Components
`src/components/Layout.tsx`:
- Top navbar: logo "CollabStack", links to `/documents`, logout button
- `<Outlet />` for page content

`src/components/ProtectedRoute.tsx` — from instructions template

`src/components/DocumentCard.tsx`:
- shadcn/ui Card showing: title, owner, collaborator count, last updated
- Buttons: Open, Delete (with confirmation Dialog)

`src/components/CollaboratorBadge.tsx`:
- Small avatar-like badge showing a user's initials + color (based on username hash)
- Used to show who is currently editing

`src/components/ChatSidebar.tsx`:
```tsx
// Props: documentId: string
// State: messages (array of {role: 'user'|'assistant', content: string, snippets?: string[]}), input
// On submit: calls chatWithDocument(), appends Q+A to messages
// Display: scrollable chat history, each answer shows source snippets as collapsed accordion
// Uses shadcn: Input, Button, ScrollArea, Accordion
```

`src/components/PresenceBar.tsx`:
- Shows `{n} people editing` + CollaboratorBadge for each connected user
- Receives `connectedUsers: string[]` as prop

### Pages
`src/pages/LoginPage.tsx`:
- shadcn/ui Card centered on page
- Form: Email, Password inputs + Login button
- Link to `/register`
- On success: store token, navigate to `/documents`

`src/pages/RegisterPage.tsx`:
- Same style as Login
- Fields: Username, Email, Password, Confirm Password
- Client-side validation: passwords match, email format

`src/pages/DocumentListPage.tsx`:
- Header: "My Documents" + "New Document" Button (opens Dialog for title input)
- Grid of `DocumentCard` components using `useDocuments` hook
- Empty state: friendly message + CTA to create first doc

`src/pages/EditorPage.tsx`:
This is the most important page. Structure:
```tsx
// Layout: full-height flex row
// Left panel (flex-1): TipTap editor
// Right panel (w-80, collapsible): ChatSidebar

const EditorPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const [document, setDocument] = useState<DocumentResponse | null>(null);
  const [isEditingTitle, setIsEditingTitle] = useState(false);
  const editorRef = useRef<Editor | null>(null);

  // 1. Load document via TanStack Query
  // 2. Connect WebSocket via useDocumentWebSocket
  // 3. Initialize TipTap editor with document.contentSnapshot as initial content
  // 4. On local editor change: convert to OperationMessage and sendOperation()
  // 5. On remoteOperation received: apply to editor without triggering another send
  //    (use a ref flag `isApplyingRemote` to prevent echo)
  // 6. Show PresenceBar with connected users
  // 7. Editable title inline (click to edit, blur to save via updateTitle API)

  // TipTap setup:
  const editor = useEditor({
    extensions: [StarterKit],
    content: document?.contentSnapshot ?? '',
    onUpdate: ({ editor }) => {
      if (isApplyingRemote.current) return;
      // Convert change to operation and send
    },
  });
}
```

### App Router
`src/App.tsx`:
```tsx
<QueryClientProvider client={queryClient}>
  <BrowserRouter>
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route element={<ProtectedRoute><Layout /></ProtectedRoute>}>
        <Route path="/documents" element={<DocumentListPage />} />
        <Route path="/documents/:id" element={<EditorPage />} />
        <Route path="/" element={<Navigate to="/documents" />} />
      </Route>
    </Routes>
  </BrowserRouter>
</QueryClientProvider>
```

### Environment
`frontend/collab-editor/.env.local`:
```
VITE_API_BASE_URL=http://localhost:8080/api
VITE_WS_BASE_URL=ws://localhost:8080
```

## Done When
- [ ] Login and register work end-to-end
- [ ] Document list shows all docs, create new doc opens dialog
- [ ] Editor page loads document content correctly
- [ ] Opening same document in two browser tabs — typing in one appears in the other
- [ ] Connected user list updates when second tab joins/leaves
- [ ] Chat sidebar sends a question, receives and displays AI answer with source snippets
- [ ] Inline title editing works (click title, type, blur → saved)
- [ ] All loading states show spinners or skeletons
- [ ] All error states show toast notifications (use shadcn/ui `useToast`)