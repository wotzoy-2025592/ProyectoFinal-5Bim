package com.gestionbiblioteca.Evaluacion.service;

import com.gestionbiblioteca.Evaluacion.dto.LibroRequest;
import com.gestionbiblioteca.Evaluacion.dto.LibroResponse;
import com.gestionbiblioteca.Evaluacion.dto.PageResponse;

public interface LibroService {

    PageResponse<LibroResponse> listar(String titulo, String categoria, int page, int size);

    LibroResponse obtener(Long id);

    LibroResponse crear(LibroRequest request);

    LibroResponse actualizar(Long id, LibroRequest request);

    void eliminar(Long id);
}
