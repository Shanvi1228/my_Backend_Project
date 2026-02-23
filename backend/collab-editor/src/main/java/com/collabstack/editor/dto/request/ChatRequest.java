package com.collabstack.editor.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        @NotBlank String question
) {}
