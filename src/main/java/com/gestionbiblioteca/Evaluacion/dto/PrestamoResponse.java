package com.gestionbiblioteca.Evaluacion.dto;

import com.gestionbiblioteca.Evaluacion.entity.EstadoPrestamo;

import java.time.LocalDate;

public record PrestamoResponse(
        Long id,
        Long usuarioId,
        String usuarioNombre,
        Long libroId,
        String libroTitulo,
        LocalDate fechaPrestamo,
        LocalDate fechaDevolucionEsperada,
        LocalDate fechaDevolucionReal,
        EstadoPrestamo estado
) {
}
