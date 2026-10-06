package com.gestionbiblioteca.Evaluacion.dto;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path
) {
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS), status, error, message, path);
    }
}
