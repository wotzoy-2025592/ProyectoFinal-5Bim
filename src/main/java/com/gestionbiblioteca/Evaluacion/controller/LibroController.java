package com.gestionbiblioteca.Evaluacion.controller;

import com.gestionbiblioteca.Evaluacion.dto.LibroRequest;
import com.gestionbiblioteca.Evaluacion.dto.LibroResponse;
import com.gestionbiblioteca.Evaluacion.dto.PageResponse;
import com.gestionbiblioteca.Evaluacion.service.LibroService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/libros")
@RequiredArgsConstructor
public class LibroController {

    private final LibroService libroService;

    @GetMapping
    public PageResponse<LibroResponse> listar(
            @RequestParam(name = "titulo", required = false) String titulo,
            @RequestParam(name = "categoria", required = false) String categoria,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        return libroService.listar(titulo, categoria, page, size);
    }

    @GetMapping("/{id}")
    public LibroResponse obtener(@PathVariable("id") Long id) {
        return libroService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LibroResponse crear(@Valid @RequestBody LibroRequest request) {
        return libroService.crear(request);
    }

    @PutMapping("/{id}")
    public LibroResponse actualizar(@PathVariable("id") Long id, @Valid @RequestBody LibroRequest request) {
        return libroService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable("id") Long id) {
        libroService.eliminar(id);
    }
}
