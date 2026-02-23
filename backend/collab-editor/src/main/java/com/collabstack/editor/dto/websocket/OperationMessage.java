package com.collabstack.editor.dto.websocket;

public record OperationMessage(
        String type,           // "OPERATION" | "PRESENCE" | "SYNC"
        OperationType opType,  // INSERT or DELETE
        int position,
        String content,        // for INSERT
        int length,            // for DELETE
        long clientRevision,   // revision client thinks it's at
        String userId,
        String username
) {}
