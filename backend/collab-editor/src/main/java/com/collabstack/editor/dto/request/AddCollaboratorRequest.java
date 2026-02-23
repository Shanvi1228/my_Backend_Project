package com.collabstack.editor.dto.request;

import com.collabstack.editor.entity.CollaboratorRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddCollaboratorRequest(
        @NotBlank @Email String email,
        @NotNull CollaboratorRole role
) {}
