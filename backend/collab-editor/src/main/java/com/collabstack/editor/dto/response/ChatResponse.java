package com.collabstack.editor.dto.response;

import java.util.List;

public record ChatResponse(
        String answer,
        List<String> sourceSnippets
) {}
