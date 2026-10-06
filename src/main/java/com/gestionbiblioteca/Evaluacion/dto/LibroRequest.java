package com.gestionbiblioteca.Evaluacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record LibroRequest(
        @NotBlank(message = "El ISBN es obligatorio")
        @Size(max = 20, message = "El ISBN admite máximo 20 caracteres")
        String isbn,

        @NotBlank(message = "El título es obligatorio")
        @Size(max = 200, message = "El título admite máximo 200 caracteres")
        String titulo,

        @NotBlank(message = "El autor es obligatorio")
        @Size(max = 150, message = "El autor admite máximo 150 caracteres")
        String autor,

        @NotBlank(message = "La categoría es obligatoria")
        @Size(max = 100, message = "La categoría admite máximo 100 caracteres")
        String categoria,

        @NotNull(message = "El stock total es obligatorio")
        @PositiveOrZero(message = "El stock total no puede ser negativo")
        Integer stockTotal,

        /** Opcional. En la creación se asume igual a stockTotal. */
        @PositiveOrZero(message = "El stock disponible no puede ser negativo")
        Integer stockDisponible
) {
}
