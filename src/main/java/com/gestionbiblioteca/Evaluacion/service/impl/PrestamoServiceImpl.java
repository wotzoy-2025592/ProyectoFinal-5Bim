package com.gestionbiblioteca.Evaluacion.service.impl;

import com.gestionbiblioteca.Evaluacion.dto.PageResponse;
import com.gestionbiblioteca.Evaluacion.dto.PrestamoRequest;
import com.gestionbiblioteca.Evaluacion.dto.PrestamoResponse;
import com.gestionbiblioteca.Evaluacion.entity.EstadoPrestamo;
import com.gestionbiblioteca.Evaluacion.entity.EstadoUsuario;
import com.gestionbiblioteca.Evaluacion.entity.Libro;
import com.gestionbiblioteca.Evaluacion.entity.Prestamo;
import com.gestionbiblioteca.Evaluacion.entity.Rol;
import com.gestionbiblioteca.Evaluacion.entity.Usuario;
import com.gestionbiblioteca.Evaluacion.exception.BusinessRuleException;
import com.gestionbiblioteca.Evaluacion.exception.ResourceNotFoundException;
import com.gestionbiblioteca.Evaluacion.mapper.PrestamoMapper;
import com.gestionbiblioteca.Evaluacion.repository.LibroRepository;
import com.gestionbiblioteca.Evaluacion.repository.PrestamoRepository;
import com.gestionbiblioteca.Evaluacion.repository.UsuarioRepository;
import com.gestionbiblioteca.Evaluacion.service.PrestamoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PrestamoServiceImpl implements PrestamoService {

    static final int PLAZO_DIAS = 14;
    static final int MAX_PRESTAMOS_ACTIVOS = 3;

    private final PrestamoRepository prestamoRepository;
    private final UsuarioRepository usuarioRepository;
    private final LibroRepository libroRepository;
    private final PrestamoMapper prestamoMapper;

    /**
     * Crea un préstamo en UNA transacción: validar reglas -> crear préstamo -> reducir stock.
     *
     * noRollbackFor = BusinessRuleException: cuando se detecta un atraso se persiste la sanción
     * del usuario (y el marcado de atrasados) y aun así se rechaza el préstamo con 400. Todas las
     * validaciones ocurren ANTES de modificar stock o crear el préstamo, por lo que no queda
     * ningún cambio parcial.
     *
     * Concurrencia: bloqueo pesimista (SELECT ... FOR UPDATE) sobre usuario y libro, siempre en
     * el mismo orden (usuario -> libro) para evitar deadlocks.
     */
    @Override
    @Transactional(noRollbackFor = BusinessRuleException.class)
    public PrestamoResponse crear(PrestamoRequest request) {
        LocalDate hoy = LocalDate.now();
        actualizarAtrasados(hoy);

        Usuario usuario = usuarioRepository.findByIdForUpdate(request.usuarioId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id " + request.usuarioId()));

        if (usuario.getRol() != Rol.LECTOR) {
            throw new BusinessRuleException("Solo se pueden registrar préstamos a usuarios con rol LECTOR");
        }

        // Regla 5: con un préstamo atrasado -> SANCIONADO y préstamo rechazado
        if (prestamoRepository.existsByUsuarioIdAndEstado(usuario.getId(), EstadoPrestamo.ATRASADO)) {
            usuario.setEstado(EstadoUsuario.SANCIONADO);
            usuarioRepository.save(usuario);
            throw new BusinessRuleException(
                    "El usuario tiene préstamos atrasados y ha sido sancionado; no puede solicitar nuevos préstamos");
        }
        if (usuario.getEstado() == EstadoUsuario.SANCIONADO) {
            throw new BusinessRuleException("El usuario está sancionado y no puede solicitar préstamos");
        }

        // Regla 2: máximo 3 préstamos activos (ACTIVO o ATRASADO = aún no devueltos)
        long activos = prestamoRepository.countByUsuarioIdAndEstadoIn(
                usuario.getId(), List.of(EstadoPrestamo.ACTIVO, EstadoPrestamo.ATRASADO));
        if (activos >= MAX_PRESTAMOS_ACTIVOS) {
            throw new BusinessRuleException("El usuario ya tiene " + MAX_PRESTAMOS_ACTIVOS + " préstamos activos");
        }

        // Regla 1: stock
        Libro libro = libroRepository.findByIdForUpdate(request.libroId())
                .orElseThrow(() -> new ResourceNotFoundException("Libro no encontrado con id " + request.libroId()));
        if (libro.getStockDisponible() <= 0) {
            throw new BusinessRuleException("No hay ejemplares disponibles del libro (stock 0)");
        }

        libro.setStockDisponible(libro.getStockDisponible() - 1);

        // Regla 3: plazo de 14 días
        Prestamo prestamo = prestamoRepository.save(Prestamo.builder()
                .usuario(usuario)
                .libro(libro)
                .fechaPrestamo(hoy)
                .fechaDevolucionEsperada(hoy.plusDays(PLAZO_DIAS))
                .estado(EstadoPrestamo.ACTIVO)
                .build());
        return prestamoMapper.toResponse(prestamo);
    }

    /** Devolución en UNA transacción: validar -> marcar DEVUELTO -> fecha real -> stock++. */
    @Override
    @Transactional
    public PrestamoResponse devolver(Long prestamoId) {
        LocalDate hoy = LocalDate.now();
        actualizarAtrasados(hoy);

        Prestamo prestamo = prestamoRepository.findByIdForUpdate(prestamoId)
                .orElseThrow(() -> new ResourceNotFoundException("Préstamo no encontrado con id " + prestamoId));
        if (prestamo.getEstado() == EstadoPrestamo.DEVUELTO) {
            throw new BusinessRuleException("El préstamo ya fue devuelto");
        }

        // Mismo orden de bloqueo que en crear(): usuario -> libro
        Usuario usuario = usuarioRepository.findByIdForUpdate(prestamo.getUsuario().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario del préstamo no encontrado"));
        Libro libro = libroRepository.findByIdForUpdate(prestamo.getLibro().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Libro del préstamo no encontrado"));

        prestamo.setFechaDevolucionReal(hoy);
        prestamo.setEstado(EstadoPrestamo.DEVUELTO);
        libro.setStockDisponible(Math.min(libro.getStockDisponible() + 1, libro.getStockTotal()));

        // Si ya no le quedan atrasos, se levanta la sanción
        if (usuario.getEstado() == EstadoUsuario.SANCIONADO
                && !prestamoRepository.existsByUsuarioIdAndEstado(usuario.getId(), EstadoPrestamo.ATRASADO)) {
            usuario.setEstado(EstadoUsuario.ACTIVO);
        }
        return prestamoMapper.toResponse(prestamo);
    }

    @Override
    @Transactional
    public PageResponse<PrestamoResponse> misPrestamos(String email, EstadoPrestamo estado, int page, int size) {
        actualizarAtrasados(LocalDate.now());
        Usuario usuario = usuarioRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "fechaPrestamo", "id"));
        Page<Prestamo> result = (estado == null)
                ? prestamoRepository.findByUsuarioId(usuario.getId(), pageable)
                : prestamoRepository.findByUsuarioIdAndEstado(usuario.getId(), estado, pageable);
        return PageResponse.from(result.map(prestamoMapper::toResponse));
    }

    @Override
    @Transactional
    public PageResponse<PrestamoResponse> atrasados(int page, int size) {
        actualizarAtrasados(LocalDate.now());
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.ASC, "fechaDevolucionEsperada", "id"));
        return PageResponse.from(
                prestamoRepository.findByEstado(EstadoPrestamo.ATRASADO, pageable).map(prestamoMapper::toResponse));
    }

    @Override
    @Transactional
    public int actualizarPrestamosAtrasados() {
        return actualizarAtrasados(LocalDate.now());
    }

    /** Regla 4: fecha actual > fechaDevolucionEsperada y no devuelto => ATRASADO. */
    private int actualizarAtrasados(LocalDate hoy) {
        return prestamoRepository.actualizarEstadoVencidos(EstadoPrestamo.ATRASADO, EstadoPrestamo.ACTIVO, hoy);
    }
}
