---
agent: agent
description: "Phase 05 – RAG-powered AI chat using Spring AI + PGVector, document as context"
tools: [codebase, editFiles]
---

Reference [copilot-instructions.md](../copilot-instructions.md). Phase 04 must be complete.

## Goal
Users can ask questions about the current document and get AI-generated answers grounded in the document's content.

## Architecture
1. On document save (or every 30s): split content into chunks → embed → store in `document_chunks`
2. On chat request: embed question → similarity search in `document_chunks` for that docId → build prompt with retrieved chunks → call LLM → return answer with source snippets

## Files to Create

### Config
`config/SpringAiConfig.java`:
- `@Bean VectorStore vectorStore(EmbeddingModel embeddingModel, JdbcTemplate jdbcTemplate)` — use `PgVectorStore`
- `@Bean ChatClient chatClient(ChatModel chatModel)` — use `ChatClient.builder(chatModel).build()`

### DTOs
`dto/request/ChatRequest.java` — record: question (String)
`dto/response/ChatResponse.java` — record: answer (String), sourceSnippets (List<String>)
`dto/response/ChunkResponse.java` — record: chunkIndex (int), contentPreview (String)

### Entity
`entity/DocumentChunk.java`:
- UUID id, UUID documentId (not a FK join — just UUID column), int chunkIndex, String content
- No embedding column in JPA entity (managed by Spring AI VectorStore directly)

### Service
`service/EmbeddingService.java` interface:
```java
void indexDocument(UUID documentId, String fullContent);
void reindexChunk(UUID documentId, int chunkIndex, String chunkContent);
void deleteDocumentChunks(UUID documentId);
```

`service/impl/EmbeddingServiceImpl.java`:
- `indexDocument`: split text into chunks of ~800 characters with 100-char overlap
  - `List<String> chunks = chunkText(content, 800, 100)`
  - Delete existing chunks for this docId from vector store
  - For each chunk: create `Document` object (Spring AI) with metadata `{documentId, chunkIndex}`
  - Call `vectorStore.add(documents)`
- Chunking helper: `List<String> chunkText(String text, int size, int overlap)`

`service/RagChatService.java` interface:
```java
ChatResponse chat(UUID documentId, UUID userId, ChatRequest request);
```

`service/impl/RagChatServiceImpl.java`:
```java
public ChatResponse chat(UUID documentId, UUID userId, ChatRequest request) {
    // 1. Verify user has access to document
    documentService.findById(documentId, userId); // throws if no access

    // 2. Retrieve relevant chunks via similarity search
    List<Document> relevantDocs = vectorStore.similaritySearch(
        SearchRequest.query(request.question())
            .withTopK(6)
            .withFilterExpression("documentId == '" + documentId + "'")
    );

    // 3. Build context string
    String context = relevantDocs.stream()
        .map(Document::getContent)
        .collect(Collectors.joining("\n\n---\n\n"));

    // 4. Build prompt
    String systemPrompt = """
        You are a helpful assistant answering questions about a document.
        Answer ONLY using the provided document context below.
        If the answer is not in the context, say "I couldn't find that in the document."
        Be concise and cite specific parts of the document when relevant.
        
        DOCUMENT CONTEXT:
        """ + context;

    // 5. Call LLM
    String answer = chatClient.prompt()
        .system(systemPrompt)
        .user(request.question())
        .call()
        .content();

    // 6. Return answer + source snippets (first 150 chars of each chunk)
    List<String> snippets = relevantDocs.stream()
        .map(d -> d.getContent().substring(0, Math.min(150, d.getContent().length())) + "...")
        .toList();

    return new ChatResponse(answer, snippets);
}
```

### Controller
`controller/ChatController.java`:
```
POST /api/documents/{documentId}/chat  → ragChatService.chat()
POST /api/documents/{documentId}/index → embeddingService.indexDocument() (manual trigger for dev/testing)
```

### Integration with Document Save
In `DocumentServiceImpl`, after updating `contentSnapshot`, call `embeddingService.indexDocument()` asynchronously with `@Async`.

In `DocumentWebSocketHandler.saveSnapshot()`, also trigger reindexing.

## Done When
- [ ] `POST /api/documents/{id}/index` re-chunks and embeds the document into PGVector
- [ ] `POST /api/documents/{id}/chat` with `{question: "..."}` returns a grounded answer
- [ ] Answer is based only on document content (not hallucinated general knowledge)
- [ ] `sourceSnippets` in response shows which parts of doc were used
- [ ] Chunks filtered by documentId (not returning chunks from other documents)