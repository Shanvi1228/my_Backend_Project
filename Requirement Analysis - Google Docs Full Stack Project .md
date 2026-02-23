## **Tech stack choices (both projects)**

* **Backend (Java)**

  * Java 21, Spring Boot

  * Spring Web (REST), Spring WebSocket (for realtime), Spring Security (JWT)

  * Spring Data JPA with PostgreSQL

  * Spring AI + PGVector/Postgres for RAG (for the editor project)[[youtube](https://www.youtube.com/watch?v=HUro1JBynyk)]​sohamkamani+1

* **Frontend (React)**

  * React + TypeScript

  * Editor: TipTap / Slate / Quill for rich text

  * WebSocket client (native WebSocket or `socket.io-client`)

  * UI: MUI / Tailwind

* **Infra / tooling**

  * Docker / Docker Compose (run db, vector db, multiple storage nodes)

  * GitHub with good README and sequence diagrams

---

## **Project 1: Realtime Collaboration Editor + RAG Chat**

Aim: “Google Docs‑like” collaborative document editor, with a chat sidebar that answers questions using the current document as context (RAG).

## **Core features to implement**

* Multi-user document editing with live updates (same doc, multiple browser tabs).

* Cursor presence (who is online in this doc).

* Basic version history (last N snapshots or operation log).

* RAG chat panel that:

  * Uses only the current document as knowledge.

  * Answers user questions with references to document sections.

---

## **Collaboration backend architecture**

At your scale, use a **central server with Operational Transform (OT)–style logic**, not full-blown CRDT.arxiv+2

**Main components (Spring Boot):**

* `AuthService`

  * JWT login/register

* `DocumentService`

  * CRUD for documents (REST)

  * Stores current snapshot + metadata in Postgres

* `CollaborationService`

  * WebSocket endpoint: `/ws/documents/{docId}`

  * Maintains in‑memory state: current text + list of connected users

  * Applies transforms to incoming operations (simple OT) and broadcasts them

* `OperationStore`

  * Persist operations (insert/delete) to `document_operations` table for recovery/history

**Rough data model (Postgres):**

* `users` (id, name, email, password_hash, …)

* `documents` (id, owner_id, title, content_snapshot, updated_at)

* `document_collaborators` (doc_id, user_id, role)

* `document_operations` (id, doc_id, user_id, op_type, position, text, timestamp, revision)

**Editing flow:**

1. User hits `/api/documents/{id}` via REST → gets:

   * Current snapshot content

   * Document metadata

2. Frontend opens WebSocket to `/ws/documents/{id}` with JWT.

3. When user types:

   * Editor converts changes into operations: `{type: "insert", position: 123, text: "a"}`

   * Sends to backend via WebSocket.

4. Backend:

   * Validates op, transforms it against any ops not yet applied by that client (simple OT).dev+1

   * Applies to server’s canonical text.

   * Broadcasts the transformed operation to all clients.

   * Persists op in `document_operations` (and periodically stores a snapshot in `documents.content_snapshot`).

You do not need a perfect academic OT; for a resume project, **position-based ops + revision numbers** are enough to show understanding.

---

## **OT vs CRDT (what to say in your README)**

* OT: Used in many real co-editors; server is the authority; operations are transformed to keep all clients consistent.tiny+2

* CRDT: More distributed, often used for offline-first/peer-to-peer, but more complex to implement well.daydreamsoft+1

* For this project, explicitly write in README: “Chose OT-style centralised approach because it matches client–server architecture and is simpler to reason about than CRDT for a single-node system.”

That sentence alone signals you know the tradeoffs.

---

## **RAG chat architecture (document-aware chat)**

You want a classic RAG pipeline: **chunk document → store embeddings → retrieve relevant chunks → send to LLM with user query**.sohamkamani+2[[youtube](https://www.youtube.com/watch?v=HUro1JBynyk)]​

**Extra components (Spring Boot + Spring AI):**

* `EmbeddingService`

  * Uses Spring AI to call an embedding model (OpenAI or local) for text chunks.spring+1

* `VectorStore`

  * Postgres + PGVector extension; Spring AI has integrations for this.[[youtube](https://www.youtube.com/watch?v=HUro1JBynyk)]​sohamkamani+1

  * Table `document_chunks` (id, doc_id, chunk_index, text, embedding_vector)

* `RagChatService`

  * Given `(docId, question)`:

    1. Embed the question.

    2. Run similarity search (top-k=4–8 chunks) limited to that `docId`.[[sohamkamani](https://www.sohamkamani.com/java/spring-ai-rag-application/)]​[[youtube](https://www.youtube.com/watch?v=HUro1JBynyk)]​

    3. Concatenate chunk texts into `context`.

    4. Build prompt: system message = “Answer strictly from this document context…”, user message = question + context.[[docs.spring](https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html)]​

    5. Call LLM via Spring AI `ChatClient`; return answer.[[youtube](https://www.youtube.com/watch?v=jClr5LtQFIQ)]​sohamkamani+1

**Flows:**

* **On doc save (or every N seconds):**

  * Backend splits `content_snapshot` into chunks (e.g., 512–1k tokens).

  * For each chunk, compute embedding and store/update in `document_chunks` with vector column.

* **Chat request:**

  * Frontend hits `/api/documents/{id}/chat` with `{question: "…"}`.

  * Backend runs retrieval + generation pipeline and returns answer.

---

## **React frontend for the editor**

**Main pages:**

* `/login`, `/register`

* `/documents` – list, create, delete.

* `/documents/:id` – editor + chat.

**Editor page layout:**

* Left: rich text editor (TipTap/Slate).

* Right: chat sidebar (display Q&A, loading states).

**Key implementation points:**

* Maintain local document state, but always:

  * Apply remote operations from WebSocket to editor.

  * Convert local changes into minimal operations and send to backend.

* Show online users (via WebSocket presence messages).

* Chat:

  * Simple chat box that calls REST API.

  * Optionally stream answer using SSE/WebSocket later as a stretch goal.

---

## **Project 2: Distributed, Encrypted, Fault-Tolerant Cloud Storage**

Aim: a small S3-like storage system with **chunking, encryption, and replication across multiple nodes** so that node failures do not lose data.scholar9+3

Do not try to build a production-grade system; instead, show a **clear, layered design** and a working demo with 3–4 nodes via Docker Compose.

---

## **High-level architecture**

Conceptually, follow designs from distributed cloud storage research: **separate metadata from data, replicate/encode chunks, detect failures, and rebuild**.ijset+3

**Services (Spring Boot microservices):**

1. **API Gateway / Metadata Service**

   * REST API for clients (React):

     * `POST /files` (upload)

     * `GET /files` (list)

     * `GET /files/{id}` (download)

   * Decides how to split, encrypt, and place chunks.

   * Stores metadata in Postgres:

     * `files` (file_id, owner, filename, size, created_at)

     * `file_chunks` (chunk_id, file_id, index, node_id, checksum)

     * `encrypted_keys` (file_id, encrypted_DEK)

2. **Storage Node Service** (multiple instances)

   * Each node has:

     * Local storage directory (mounted volume).

     * REST endpoints:

       * `PUT /chunks/{chunkId}` – store encrypted chunk.

       * `GET /chunks/{chunkId}` – return encrypted chunk.

       * `GET /health` – heartbeat.

   * Run 3–4 instances with different ports in Docker Compose to simulate cluster.

3. **Replication / Health Manager**

   * Periodic job in metadata service:

     * Ping each storage node via `/health`.

     * Mark nodes as `UP`/`DOWN`.

     * For each chunk on a `DOWN` node, replicate from another `UP` replica to a new `UP` node until replication factor (e.g., 3) is restored.geeksforgeeks+1

---

## **Encryption design**

Keep it **per-file encryption with a Data Encryption Key (DEK)**.

* When uploading a file:

  * Generate random DEK (e.g., AES-256 key).

  * Derive a Key Encryption Key (KEK) from the user password (PBKDF2/Argon2) **or** use an RSA keypair.

  * Encrypt file data with DEK using AES-GCM.

  * Encrypt DEK with KEK and store only the encrypted DEK in `encrypted_keys`.arxiv+1

* Storage nodes only ever see **opaque encrypted bytes**; they cannot decrypt files.

This lets you talk about **encryption at rest** and **separation of key and data** in your resume.

---

## **Data placement and fault tolerance**

Use a **replication-based approach** (not full erasure coding) for implementation simplicity, but reference erasure coding in README as potential improvement.scholar9+1

* Choose a replication factor R=3R = 3R=3.

* For each chunk:

  * Use consistent hashing or simple round‑robin across nodes to pick 3 nodes.

  * Store `(file_id, chunk_index, node_ids)` in `file_chunks`.

* On node failure (health check fails repeatedly):

  * Mark node as DOWN.

  * For each chunk where one replica is on the failed node:

    * Fetch another replica from a healthy node.

    * Store it on a new healthy node.

    * Update `file_chunks` mapping.

This lets you demonstrate **fault tolerance via replication and background repair**, a common pattern in real systems.geeksforgeeks+2

---

## **Upload and download flows**

**Upload:**

1. React sends file to `/api/files` (multipart).

2. Gateway:

   * Generates DEK + encrypts file stream in chunks (e.g., 4 MB).

   * For each chunk:

     * Compute checksum.

     * Choose 3 storage nodes and `PUT /chunks/{chunkId}` to each.

     * Insert rows in `file_chunks`.

   * Store metadata + encrypted DEK.

3. Return `fileId` + summary (nodes, chunk count).

**Download:**

1. React requests `/api/files/{fileId}`.

2. Gateway:

   * Fetches chunk list and encrypted DEK.

   * Decrypts DEK with user KEK.

   * For each chunk index:

     * Pick any healthy node from replica set.

     * `GET /chunks/{chunkId}`, verify checksum.

   * Decrypt chunks with DEK, stream back to client.

**Node failure demo:**

* Bring down one storage-node container.

* Hit a “rebalance” endpoint or wait for scheduled repair job.

* Show that `file_chunks` has been updated to use remaining healthy nodes and you can still download the file.

---

## **React frontend for storage**

Keep UI simple but polished:

* **Auth page** (reuse JWT from editor project if you like).

* **Files page**

  * Upload button (shows progress).

  * Table with filename, size, created_at, replica count, health status.

  * Download button.

* **Admin/Debug view** (optional but great for interviews)

  * List of nodes with status (UP/DOWN).

  * For a selected file, list which chunks live on which nodes.

---

