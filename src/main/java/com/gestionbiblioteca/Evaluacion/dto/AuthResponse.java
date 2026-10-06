package com.gestionbiblioteca.Evaluacion.dto;

import com.gestionbiblioteca.Evaluacion.entity.Rol;

public record AuthResponse(
        String token,
        String tipo,
        long expiraEnMs,
        Long id,
        String nombre,
        String email,
        Rol rol
) {
}
