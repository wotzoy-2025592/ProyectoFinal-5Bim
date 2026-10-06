package com.gestionbiblioteca.Evaluacion.mapper;

import com.gestionbiblioteca.Evaluacion.dto.PrestamoResponse;
import com.gestionbiblioteca.Evaluacion.entity.Prestamo;
import org.springframework.stereotype.Component;

@Component
public class PrestamoMapper {

    public PrestamoResponse toResponse(Prestamo p) {
        return new PrestamoResponse(
                p.getId(),
                p.getUsuario().getId(),
                p.getUsuario().getNombre(),
                p.getLibro().getId(),
                p.getLibro().getTitulo(),
                p.getFechaPrestamo(),
                p.getFechaDevolucionEsperada(),
                p.getFechaDevolucionReal(),
                p.getEstado());
    }
}
