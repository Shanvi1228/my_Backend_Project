package com.collabstack.editor.service;

import com.collabstack.editor.dto.request.ChatRequest;
import com.collabstack.editor.dto.response.ChatResponse;

import java.util.UUID;

public interface RagChatService {
    ChatResponse chat(UUID documentId, UUID userId, ChatRequest request);
}
