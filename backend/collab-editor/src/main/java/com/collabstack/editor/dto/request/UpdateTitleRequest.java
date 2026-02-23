package com.collabstack.editor.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateTitleRequest(
        @NotBlank @Size(max = 500) String title
) {}
