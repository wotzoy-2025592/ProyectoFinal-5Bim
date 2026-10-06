package com.gestionbiblioteca.Evaluacion.service;

import com.gestionbiblioteca.Evaluacion.dto.PageResponse;
import com.gestionbiblioteca.Evaluacion.dto.PrestamoRequest;
import com.gestionbiblioteca.Evaluacion.dto.PrestamoResponse;
import com.gestionbiblioteca.Evaluacion.entity.EstadoPrestamo;

public interface PrestamoService {

    PrestamoResponse crear(PrestamoRequest request);

    PrestamoResponse devolver(Long prestamoId);

    /** Historial del usuario autenticado (opcionalmente filtrado por estado). */
    PageResponse<PrestamoResponse> misPrestamos(String email, EstadoPrestamo estado, int page, int size);

    PageResponse<PrestamoResponse> atrasados(int page, int size);

    /** Marca ATRASADO los préstamos vencidos. Devuelve cuántos cambió. */
    int actualizarPrestamosAtrasados();
}
