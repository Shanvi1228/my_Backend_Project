package com.collabstack.editor.dto.response;

import java.util.UUID;

public record AuthResponse(
        String token,
        UUID userId,
        String username,
        String email
) {}
