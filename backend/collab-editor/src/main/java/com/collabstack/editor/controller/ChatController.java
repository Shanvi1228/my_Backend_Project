package com.collabstack.editor.controller;

import com.collabstack.editor.dto.request.ChatRequest;
import com.collabstack.editor.dto.response.ChatResponse;
import com.collabstack.editor.exception.ApiResponse;
import com.collabstack.editor.security.UserPrincipal;
import com.collabstack.editor.service.DocumentService;
import com.collabstack.editor.service.EmbeddingService;
import com.collabstack.editor.service.RagChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Tag(name = "RAG Chat", description = "AI-powered document chat via RAG pipeline")
@SecurityRequirement(name = "bearerAuth")
public class ChatController {

    private final RagChatService ragChatService;
    private final EmbeddingService embeddingService;
    private final DocumentService documentService;

    @PostMapping("/{documentId}/chat")
    @Operation(summary = "Ask a question about a document (RAG-powered)")
    public ResponseEntity<ApiResponse<ChatResponse>> chat(
            @PathVariable UUID documentId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChatRequest request) {
        ChatResponse response = ragChatService.chat(documentId, principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{documentId}/index")
    @Operation(summary = "Manually trigger document re-indexing into the vector store")
    public ResponseEntity<ApiResponse<Void>> indexDocument(
            @PathVariable UUID documentId,
            @AuthenticationPrincipal UserPrincipal principal) {
        // Verify access
        String content = documentService.findById(documentId, principal.getId()).contentSnapshot();
        embeddingService.indexDocument(documentId, content);
        return ResponseEntity.ok(ApiResponse.ok("Indexing started asynchronously", null));
    }
}
