package com.gestionbiblioteca.Evaluacion.controller;

import com.gestionbiblioteca.Evaluacion.dto.PageResponse;
import com.gestionbiblioteca.Evaluacion.dto.PrestamoRequest;
import com.gestionbiblioteca.Evaluacion.dto.PrestamoResponse;
import com.gestionbiblioteca.Evaluacion.entity.EstadoPrestamo;
import com.gestionbiblioteca.Evaluacion.service.PrestamoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/prestamos")
@RequiredArgsConstructor
public class PrestamoController {

    private final PrestamoService prestamoService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PrestamoResponse crear(@Valid @RequestBody PrestamoRequest request) {
        return prestamoService.crear(request);
    }

    @PatchMapping("/{id}/devolucion")
    public PrestamoResponse devolver(@PathVariable("id") Long id) {
        return prestamoService.devolver(id);
    }

    /** Solo los préstamos del usuario autenticado (historial; filtrable con ?estado=ACTIVO). */
    @GetMapping("/mis-prestamos")
    public PageResponse<PrestamoResponse> misPrestamos(
            Authentication authentication,
            @RequestParam(name = "estado", required = false) EstadoPrestamo estado,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        return prestamoService.misPrestamos(authentication.getName(), estado, page, size);
    }

    @GetMapping("/atrasados")
    public PageResponse<PrestamoResponse> atrasados(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        return prestamoService.atrasados(page, size);
    }
}
