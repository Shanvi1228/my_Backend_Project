package com.collabstack.editor.dto.websocket;

public record PresenceMessage(
        String type,      // "PRESENCE"
        String userId,
        String username,
        String event      // "JOIN" | "LEAVE"
) {}
