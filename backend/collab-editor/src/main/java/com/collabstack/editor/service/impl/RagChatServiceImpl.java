package com.collabstack.editor.service.impl;

import com.collabstack.editor.dto.request.ChatRequest;
import com.collabstack.editor.dto.response.ChatResponse;
import com.collabstack.editor.service.DocumentService;
import com.collabstack.editor.service.RagChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagChatServiceImpl implements RagChatService {

    private final DocumentService documentService;
    private final ChatClient chatClient;

    // Optional — only available when app.rag.enabled=true
    @Autowired(required = false)
    private VectorStore vectorStore;

    @Override
    public ChatResponse chat(UUID documentId, UUID userId, ChatRequest request) {
        // 1. Verify user has access (throws UnauthorizedException if not)
        documentService.findById(documentId, userId);

        if (vectorStore == null) {
            return new ChatResponse(
                    "RAG is not enabled on this server. Set app.rag.enabled=true and ensure pgvector is installed.",
                    Collections.emptyList()
            );
        }

        // 2. Retrieve relevant chunks via similarity search filtered by documentId
        List<Document> relevantDocs;
        try {
            relevantDocs = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(request.question())
                            .topK(6)
                            .filterExpression("documentId == '" + documentId.toString() + "'")
                            .build()
            );
        } catch (Exception e) {
            log.error("Vector similarity search failed for document {}: {}", documentId, e.getMessage());
            return new ChatResponse("Failed to retrieve context from document. Please try again.",
                    Collections.emptyList());
        }

        if (relevantDocs.isEmpty()) {
            return new ChatResponse(
                    "No indexed content found for this document. Use the /index endpoint to index it first.",
                    Collections.emptyList()
            );
        }

        // 3. Build context string
        String context = relevantDocs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n---\n\n"));

        // 4. System prompt
        String systemPrompt = """
                You are a helpful assistant answering questions about a document.
                Answer ONLY using the provided document context below.
                If the answer is not in the context, say "I couldn't find that in the document."
                Be concise and cite specific parts of the document when relevant.

                DOCUMENT CONTEXT:
                """ + context;

        // 5. Call LLM
        String answer;
        try {
            answer = chatClient.prompt()
                    .system(systemPrompt)
                    .user(request.question())
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("LLM call failed for document {}: {}", documentId, e.getMessage());
            return new ChatResponse("AI service is unavailable. Please check your OPENAI_API_KEY.",
                    Collections.emptyList());
        }

        // 6. Extract source snippets (first 150 chars of each retrieved chunk)
        List<String> snippets = relevantDocs.stream()
                .map(d -> d.getText().substring(0, Math.min(150, d.getText().length())) + "...")
                .toList();

        log.info("RAG chat answered question for document {} using {} chunks", documentId, relevantDocs.size());
        return new ChatResponse(answer, snippets);
    }
}
