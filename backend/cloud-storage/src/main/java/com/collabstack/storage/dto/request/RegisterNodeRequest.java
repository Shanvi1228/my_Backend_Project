package com.collabstack.storage.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterNodeRequest(
        @NotBlank String id,
        @NotBlank String host,
        @NotNull Integer port
) {}
