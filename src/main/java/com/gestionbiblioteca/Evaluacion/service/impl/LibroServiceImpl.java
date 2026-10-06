package com.gestionbiblioteca.Evaluacion.service.impl;

import com.gestionbiblioteca.Evaluacion.dto.LibroRequest;
import com.gestionbiblioteca.Evaluacion.dto.LibroResponse;
import com.gestionbiblioteca.Evaluacion.dto.PageResponse;
import com.gestionbiblioteca.Evaluacion.entity.Libro;
import com.gestionbiblioteca.Evaluacion.exception.BusinessRuleException;
import com.gestionbiblioteca.Evaluacion.exception.DuplicateResourceException;
import com.gestionbiblioteca.Evaluacion.exception.ResourceNotFoundException;
import com.gestionbiblioteca.Evaluacion.mapper.LibroMapper;
import com.gestionbiblioteca.Evaluacion.repository.LibroRepository;
import com.gestionbiblioteca.Evaluacion.repository.PrestamoRepository;
import com.gestionbiblioteca.Evaluacion.service.LibroService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LibroServiceImpl implements LibroService {

    private final LibroRepository libroRepository;
    private final PrestamoRepository prestamoRepository;
    private final LibroMapper libroMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<LibroResponse> listar(String titulo, String categoria, int page, int size) {
        Specification<Libro> spec = (root, query, cb) -> cb.conjunction();
        if (titulo != null && !titulo.isBlank()) {
            String patron = "%" + titulo.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("titulo")), patron));
        }
        if (categoria != null && !categoria.isBlank()) {
            String cat = categoria.trim().toLowerCase();
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("categoria")), cat));
        }
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by("titulo").ascending());
        Page<LibroResponse> result = libroRepository.findAll(spec, pageable).map(libroMapper::toResponse);
        return PageResponse.from(result);
    }

    @Override
    @Transactional(readOnly = true)
    public LibroResponse obtener(Long id) {
        return libroMapper.toResponse(buscar(id));
    }

    @Override
    @Transactional
    public LibroResponse crear(LibroRequest request) {
        String isbn = request.isbn().trim();
        if (libroRepository.existsByIsbn(isbn)) {
            throw new DuplicateResourceException("Ya existe un libro con el ISBN " + isbn);
        }
        int total = request.stockTotal();
        int disponible = request.stockDisponible() != null ? request.stockDisponible() : total;
        validarStock(total, disponible);

        Libro libro = libroRepository.save(Libro.builder()
                .isbn(isbn)
                .titulo(request.titulo().trim())
                .autor(request.autor().trim())
                .categoria(request.categoria().trim())
                .stockTotal(total)
                .stockDisponible(disponible)
                .build());
        return libroMapper.toResponse(libro);
    }

    @Override
    @Transactional
    public LibroResponse actualizar(Long id, LibroRequest request) {
        // Bloqueo pesimista: evita competir con un préstamo/devolución que modifica el stock
        Libro libro = libroRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Libro no encontrado con id " + id));

        String isbn = request.isbn().trim();
        if (libroRepository.existsByIsbnAndIdNot(isbn, id)) {
            throw new DuplicateResourceException("Ya existe otro libro con el ISBN " + isbn);
        }

        int prestados = libro.getStockTotal() - libro.getStockDisponible();
        int nuevoTotal = request.stockTotal();
        // Si no se envía stockDisponible se conserva la cantidad prestada
        int nuevoDisponible = request.stockDisponible() != null
                ? request.stockDisponible()
                : nuevoTotal - prestados;
        if (nuevoDisponible < 0) {
            throw new BusinessRuleException("El stock total (" + nuevoTotal
                    + ") no puede ser menor a los ejemplares actualmente prestados (" + prestados + ")");
        }
        validarStock(nuevoTotal, nuevoDisponible);

        libro.setIsbn(isbn);
        libro.setTitulo(request.titulo().trim());
        libro.setAutor(request.autor().trim());
        libro.setCategoria(request.categoria().trim());
        libro.setStockTotal(nuevoTotal);
        libro.setStockDisponible(nuevoDisponible);
        return libroMapper.toResponse(libroRepository.save(libro));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Libro libro = libroRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Libro no encontrado con id " + id));
        if (prestamoRepository.existsByLibroId(id)) {
            throw new BusinessRuleException("No se puede eliminar el libro porque tiene préstamos asociados");
        }
        libroRepository.delete(libro);
    }

    private Libro buscar(Long id) {
        return libroRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Libro no encontrado con id " + id));
    }

    private void validarStock(int total, int disponible) {
        if (total < 0 || disponible < 0) {
            throw new BusinessRuleException("El stock no puede ser negativo");
        }
        if (disponible > total) {
            throw new BusinessRuleException("El stock disponible no puede ser mayor que el stock total");
        }
    }
}
