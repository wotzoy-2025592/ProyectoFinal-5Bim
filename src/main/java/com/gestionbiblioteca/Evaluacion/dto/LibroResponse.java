package com.gestionbiblioteca.Evaluacion.dto;

public record LibroResponse(
        Long id,
        String isbn,
        String titulo,
        String autor,
        String categoria,
        int stockTotal,
        int stockDisponible
) {
}
