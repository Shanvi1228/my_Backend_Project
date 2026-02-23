---
applyTo: "frontend/**"
---

# Frontend-Specific Instructions

## Axios Client Setup
File: `src/api/client.ts` in BOTH frontend apps
```ts
import axios from 'axios';
import { useAuthStore } from '../store/useAuthStore';

const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 10000,
});

client.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

client.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      useAuthStore.getState().logout();
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default client;
```

## API Response Type
```ts
// types/common.types.ts
export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}
```

## Auth Store (Zustand)
```ts
// store/useAuthStore.ts
import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface AuthState {
  token: string | null;
  user: UserInfo | null;
  login: (token: string, user: UserInfo) => void;
  logout: () => void;
  isAuthenticated: () => boolean;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      token: null,
      user: null,
      login: (token, user) => set({ token, user }),
      logout: () => set({ token: null, user: null }),
      isAuthenticated: () => get().token !== null,
    }),
    { name: 'auth-storage' }
  )
);
```

## React Query Setup
Wrap `App.tsx` in:
```tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
const queryClient = new QueryClient({
  defaultOptions: { queries: { staleTime: 1000 * 60, retry: 1 } }
});
// <QueryClientProvider client={queryClient}>...</QueryClientProvider>
```

## Protected Route Pattern
```tsx
// components/ProtectedRoute.tsx
const ProtectedRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated());
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  return <>{children}</>;
};
```

## WebSocket Hook Pattern (collab-editor only)
```ts
// hooks/useDocumentWebSocket.ts
export const useDocumentWebSocket = (documentId: string) => {
  const [isConnected, setIsConnected] = useState(false);
  const socketRef = useRef<WebSocket | null>(null);
  const token = useAuthStore((s) => s.token);

  useEffect(() => {
    const ws = new WebSocket(`${import.meta.env.VITE_WS_BASE_URL}/ws/documents/${documentId}?token=${token}`);
    socketRef.current = ws;
    ws.onopen = () => setIsConnected(true);
    ws.onclose = () => setIsConnected(false);
    return () => ws.close();
  }, [documentId]);

  const sendOperation = (op: DocumentOperation) => {
    if (socketRef.current?.readyState === WebSocket.OPEN) {
      socketRef.current.send(JSON.stringify(op));
    }
  };

  return { isConnected, sendOperation, socketRef };
};
```

## Tailwind + shadcn/ui Rules
- Use shadcn/ui components (`Button`, `Input`, `Card`, `Dialog`, `Toast`) wherever possible
- Do not write custom CSS unless shadcn/ui does not cover the case
- Use Tailwind utility classes directly, no custom CSS files except `index.css`
- Dark mode support via `class` strategy in `tailwind.config.ts`

## Page Layout Rules
- All pages in `pages/` directory
- All pages wrapped in a common `<Layout>` component with nav
- Login/Register pages have no nav
- Use `<Outlet>` for nested routes

## Environment Variables
Both apps need `.env.local`:
```
VITE_API_BASE_URL=http://localhost:8080/api
VITE_WS_BASE_URL=ws://localhost:8080
```
(cloud-storage uses port 8081)