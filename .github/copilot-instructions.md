# CollabStack – GitHub Copilot Workspace Instructions

## What This Project Is
A full-stack monorepo containing two applications:
1. **collab-editor** – Google Docs–style real-time collaborative editor with OT-based sync and RAG-powered AI chat
2. **cloud-storage** – Distributed, AES-GCM–encrypted, fault-tolerant file storage with chunk replication and auto-repair

> Before generating any code, always read `Requirement Analysis - Google Docs Full Stack Project.md` in the root folder for full feature context.

---

## Monorepo Folder Structure
Generate ALL files in this exact layout. Never deviate.

```
/
├── .github/
├── backend/
│   ├── pom.xml                              ← Maven parent (multi-module)
│   ├── collab-editor/
│   │   └── src/main/java/com/collabstack/editor/
│   │       ├── CollabEditorApplication.java
│   │       ├── config/
│   │       ├── controller/
│   │       ├── dto/request/
│   │       ├── dto/response/
│   │       ├── entity/
│   │       ├── exception/
│   │       ├── repository/
│   │       ├── security/
│   │       ├── service/
│   │       └── service/impl/
│   └── cloud-storage/
│       └── src/main/java/com/collabstack/storage/
│           ├── CloudStorageApplication.java
│           ├── config/
│           ├── controller/
│           ├── dto/request/
│           ├── dto/response/
│           ├── entity/
│           ├── exception/
│           ├── repository/
│           ├── security/
│           ├── service/
│           └── service/impl/
├── storage-node/
│   └── src/main/java/com/collabstack/storagenode/
│       ├── StorageNodeApplication.java
│       ├── controller/
│       └── service/
├── frontend/
│   ├── collab-editor/src/
│   │   ├── api/
│   │   ├── components/
│   │   ├── hooks/
│   │   ├── pages/
│   │   ├── store/
│   │   ├── types/
│   │   └── utils/
│   └── cloud-storage/src/
│       ├── api/
│       ├── components/
│       ├── hooks/
│       ├── pages/
│       ├── store/
│       └── types/
├── docker-compose.yml
├── .env.example
└── Requirement Analysis - Google Docs Full Stack Project.md
```

---

## Technology Stack — Do Not Deviate

### Backend
| Concern | Choice |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.3.x |
| REST | Spring Web MVC |
| WebSocket | Spring WebSocket + STOMP |
| Security | Spring Security 6 + jjwt 0.12.x |
| ORM | Spring Data JPA + Hibernate 6 |
| AI / RAG | Spring AI 1.0.x |
| Vector store | PostgreSQL + PGVector (Spring AI integration) |
| Build | Maven multi-module |
| Utilities | Lombok, MapStruct, Springdoc OpenAPI 3, Flyway |
| Database | PostgreSQL 16 |
| Encryption | Java AES/GCM via `javax.crypto` |

### Frontend
| Concern | Choice |
|---|---|
| Framework | React 18 + TypeScript 5 |
| Build | Vite 5 |
| Rich text editor | TipTap v2 |
| Global state | Zustand 4 |
| Server state | TanStack Query v5 |
| HTTP | Axios |
| Routing | React Router v6 |
| UI | Tailwind CSS 3 + shadcn/ui |
| WebSocket | Native browser WebSocket (no socket.io) |

---

## Universal Java Coding Rules

### Response Wrapper
Every controller returns `ResponseEntity<ApiResponse<T>>`. Define once:
```java
// com.collabstack.{module}.exception.ApiResponse
public record ApiResponse<T>(boolean success, String message, T data, Instant timestamp) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "Success", data, Instant.now());
    }
    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data, Instant.now());
    }
    public static ApiResponse<Void> error(String message) {
        return new ApiResponse<>(false, message, null, Instant.now());
    }
}
```

### Entity Pattern
```java
@Entity
@Table(name = "table_name")
@EntityListeners(AuditingEntityListener.class)
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    // ... fields ...
    @CreatedDate @Column(updatable = false)
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;
}
```

### Service Pattern
Always interface + impl. Use `@RequiredArgsConstructor`:
```java
// Interface
public interface DocumentService {
    DocumentResponse create(UUID userId, DocumentCreateRequest request);
    DocumentResponse findById(UUID id, UUID requestingUserId);
}

// Impl
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentServiceImpl implements DocumentService {
    private final DocumentRepository documentRepository;
    // Never @Autowired field injection
}
```

### DTOs
Use Java records:
```java
public record DocumentCreateRequest(
    @NotBlank String title,
    String initialContent
) {}

public record DocumentResponse(
    UUID id, String title, String content,
    Long revision, Instant createdAt
) {}
```

### Exception Handling
Define these custom exceptions in `exception/` package:
- `ResourceNotFoundException(String message)` → HTTP 404
- `UnauthorizedException(String message)` → HTTP 403
- `ConflictException(String message)` → HTTP 409
- `StorageException(String message)` → HTTP 500
- `GlobalExceptionHandler` with `@RestControllerAdvice` handles all of them

### Naming Conventions
- Controllers: `UserController`, `DocumentController`
- Services: `DocumentService` (interface) + `DocumentServiceImpl`
- Repositories: `DocumentRepository`
- Entities: singular noun — `User`, `Document`, `FileMetadata`
- DTOs: `LoginRequest`, `LoginResponse`, `DocumentCreateRequest`, `DocumentResponse`
- Mappers: `DocumentMapper` (MapStruct `@Mapper(componentModel = "spring")`)

### Never Do These
- Never use field injection `@Autowired`
- Never use `var` — always explicit types
- Never put business logic in controllers
- Never call repositories from controllers directly
- Never hardcode secrets — use `${ENV_VAR}` in `application.yml`
- Never return raw entities from controllers — always map to DTOs

---

## Universal React / TypeScript Rules

### Component Template
```tsx
interface MyComponentProps {
  id: string;
  onAction?: (id: string) => void;
}

const MyComponent: React.FC<MyComponentProps> = ({ id, onAction }) => {
  return <div>{id}</div>;
};

export default MyComponent;
```

### API Client Pattern
`src/api/client.ts` → Axios instance with JWT interceptor.
Per-domain files:
```ts
// api/documents.api.ts
export const fetchDocuments = (): Promise<DocumentResponse[]> =>
  client.get<ApiResponse<DocumentResponse[]>>('/documents').then(r => r.data.data);
```

### State Rules
- Auth (user info, token) → Zustand `useAuthStore`
- Document list, file list → React Query (server state)
- Editor content + collaborator presence → component state + WebSocket sync
- No prop drilling beyond 2 levels — use Zustand or React Context

### Strict Rules
- No `any` type — ever
- No class components
- One component per file
- All API types defined in `types/` folder
- Custom hooks for all WebSocket and side-effect logic

---

## Database Schemas

### collab-editor DB
```sql
-- Table: users
id UUID PK, email UNIQUE, username, password_hash, created_at, updated_at

-- Table: documents
id UUID PK, owner_id FK users, title, content_snapshot TEXT, current_revision BIGINT, created_at, updated_at

-- Table: document_collaborators
id UUID PK, document_id FK, user_id FK, role VARCHAR(20), created_at
UNIQUE(document_id, user_id)

-- Table: document_operations
id UUID PK, document_id FK, user_id FK, op_type VARCHAR(20),
position INT, content TEXT, length INT, revision BIGINT, created_at

-- Table: document_chunks (for RAG)
id UUID PK, document_id FK, chunk_index INT, content TEXT,
embedding vector(1536), created_at
UNIQUE(document_id, chunk_index)
```

### cloud-storage DB
```sql
-- Table: users (same pattern as above)

-- Table: storage_nodes
id VARCHAR(50) PK, host, port INT, status VARCHAR(20), last_heartbeat, created_at

-- Table: file_metadata
id UUID PK, owner_id FK, original_filename, content_type, total_size_bytes BIGINT,
chunk_count INT, replication_factor INT DEFAULT 3, status VARCHAR(20), created_at, updated_at

-- Table: file_chunks
id UUID PK, file_id FK, chunk_index INT, node_id FK storage_nodes, checksum VARCHAR(64),
size_bytes INT
UNIQUE(file_id, chunk_index, node_id)

-- Table: encrypted_keys
id UUID PK, file_id FK UNIQUE, encrypted_dek TEXT, kek_salt VARCHAR(64), iv VARCHAR(32), created_at
```

---

## Docker Infrastructure
`docker-compose.yml` services:
- `postgres` → postgres:16 with pgvector, port 5432, init script creates both DBs
- `collab-editor-backend` → port 8080, depends on postgres
- `cloud-storage-backend` → port 8081, depends on postgres
- `storage-node-1` → port 9001, DATA_DIR=/data/node1, NODE_ID=node-1
- `storage-node-2` → port 9002, DATA_DIR=/data/node2, NODE_ID=node-2
- `storage-node-3` → port 9003, DATA_DIR=/data/node3, NODE_ID=node-3

All secrets via `.env` file. Never hardcode in compose file.

---

## Implementation Order
Run phase prompt files in strict order via Copilot Chat (`/phase-01-scaffold`, etc.):
1. `phase-01-scaffold` → skeleton, Maven POMs, Docker, DB migrations
2. `phase-02-auth` → JWT auth for both services
3. `phase-03-document-crud` → REST CRUD for documents
4. `phase-04-websocket-editor` → WebSocket + OT collaboration
5. `phase-05-rag-chat` → Spring AI RAG pipeline
6. `phase-06-storage-api` → Upload/download API gateway
7. `phase-07-storage-nodes` → Storage node service
8. `phase-08-encryption-replication` → AES-GCM + replication + repair
9. `phase-09-frontend-editor` → React editor UI
10. `phase-10-frontend-storage` → React storage UI