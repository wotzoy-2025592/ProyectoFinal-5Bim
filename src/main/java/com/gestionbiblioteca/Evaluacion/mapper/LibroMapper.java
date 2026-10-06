package com.gestionbiblioteca.Evaluacion.mapper;

import com.gestionbiblioteca.Evaluacion.dto.LibroResponse;
import com.gestionbiblioteca.Evaluacion.entity.Libro;
import org.springframework.stereotype.Component;

@Component
public class LibroMapper {

    public LibroResponse toResponse(Libro libro) {
        return new LibroResponse(libro.getId(), libro.getIsbn(), libro.getTitulo(), libro.getAutor(),
                libro.getCategoria(), libro.getStockTotal(), libro.getStockDisponible());
    }
}
