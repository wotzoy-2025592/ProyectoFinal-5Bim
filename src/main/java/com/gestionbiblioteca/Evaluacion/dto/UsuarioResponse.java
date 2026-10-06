package com.gestionbiblioteca.Evaluacion.dto;

import com.gestionbiblioteca.Evaluacion.entity.EstadoUsuario;
import com.gestionbiblioteca.Evaluacion.entity.Rol;

public record UsuarioResponse(
        Long id,
        String nombre,
        String email,
        EstadoUsuario estado,
        Rol rol
) {
}
