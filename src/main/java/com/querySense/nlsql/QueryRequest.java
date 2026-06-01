package com.querySense.nlsql;

import jakarta.validation.constraints.NotBlank;

public record QueryRequest(
        @NotBlank(message = "question must not be empty")
        String question
) {}