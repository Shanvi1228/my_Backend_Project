package com.collabstack.editor.dto.response;

import com.collabstack.editor.entity.CollaboratorRole;
import java.util.UUID;

public record CollaboratorResponse(
        UUID userId,
        String username,
        String email,
        CollaboratorRole role
) {}
