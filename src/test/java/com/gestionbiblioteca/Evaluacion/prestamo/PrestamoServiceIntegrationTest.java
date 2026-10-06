package com.gestionbiblioteca.Evaluacion.prestamo;

import com.gestionbiblioteca.Evaluacion.BaseIntegrationTest;
import com.gestionbiblioteca.Evaluacion.dto.PageResponse;
import com.gestionbiblioteca.Evaluacion.dto.PrestamoRequest;
import com.gestionbiblioteca.Evaluacion.dto.PrestamoResponse;
import com.gestionbiblioteca.Evaluacion.entity.*;
import com.gestionbiblioteca.Evaluacion.exception.BusinessRuleException;
import com.gestionbiblioteca.Evaluacion.repository.LibroRepository;
import com.gestionbiblioteca.Evaluacion.repository.PrestamoRepository;
import com.gestionbiblioteca.Evaluacion.repository.UsuarioRepository;
import com.gestionbiblioteca.Evaluacion.service.PrestamoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Reglas de negocio de préstamos. Cada test se revierte (@Transactional) y usa un lector nuevo. */
@Transactional
class PrestamoServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private PrestamoService prestamoService;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private LibroRepository libroRepository;
    @Autowired
    private PrestamoRepository prestamoRepository;

    private Usuario nuevoLector() {
        return usuarioRepository.save(Usuario.builder()
                .nombre("Lector Test")
                .email("lector-" + UUID.randomUUID() + "@test.com")
                .password("no-usado")
                .rol(Rol.LECTOR)
                .estado(EstadoUsuario.ACTIVO)
                .build());
    }

    private Libro nuevoLibro(int stock) {
        return libroRepository.save(Libro.builder()
                .isbn("T-" + UUID.randomUUID().toString().substring(0, 12))
                .titulo("Libro de prueba")
                .autor("Autor")
                .categoria("Test")
                .stockTotal(stock)
                .stockDisponible(stock)
                .build());
    }

    private void hacerVencido(Long prestamoId) {
        Prestamo p = prestamoRepository.findById(prestamoId).orElseThrow();
        p.setFechaPrestamo(LocalDate.now().minusDays(20));
        p.setFechaDevolucionEsperada(LocalDate.now().minusDays(6));
        prestamoRepository.saveAndFlush(p);
    }

    @Test
    void prestamoCorrecto_reduceStock_y_fija14Dias() {
        Usuario lector = nuevoLector();
        Libro libro = nuevoLibro(2);

        PrestamoResponse resp = prestamoService.crear(new PrestamoRequest(lector.getId(), libro.getId()));

        assertThat(resp.estado()).isEqualTo(EstadoPrestamo.ACTIVO);
        assertThat(resp.fechaPrestamo()).isEqualTo(LocalDate.now());
        assertThat(resp.fechaDevolucionEsperada()).isEqualTo(LocalDate.now().plusDays(14));
        assertThat(resp.fechaDevolucionReal()).isNull();
        assertThat(libroRepository.findById(libro.getId()).orElseThrow().getStockDisponible()).isEqualTo(1);
    }

    @Test
    void prestamoSinStock_seRechaza() {
        Usuario lector = nuevoLector();
        Libro libro = nuevoLibro(1);
        libro.setStockDisponible(0);
        libroRepository.saveAndFlush(libro);

        assertThatThrownBy(() -> prestamoService.crear(new PrestamoRequest(lector.getId(), libro.getId())))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("disponibles");
        assertThat(libroRepository.findById(libro.getId()).orElseThrow().getStockDisponible()).isZero();
    }

    @Test
    void cuartoPrestamo_seRechaza() {
        Usuario lector = nuevoLector();
        for (int i = 0; i < 3; i++) {
            Libro l = nuevoLibro(1);
            prestamoService.crear(new PrestamoRequest(lector.getId(), l.getId()));
        }
        Libro cuarto = nuevoLibro(1);

        assertThatThrownBy(() -> prestamoService.crear(new PrestamoRequest(lector.getId(), cuarto.getId())))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("3 préstamos activos");
        assertThat(libroRepository.findById(cuarto.getId()).orElseThrow().getStockDisponible()).isEqualTo(1);
    }

    @Test
    void usuarioSancionado_noPuedePedirPrestamos() {
        Usuario lector = nuevoLector();
        lector.setEstado(EstadoUsuario.SANCIONADO);
        usuarioRepository.saveAndFlush(lector);
        Libro libro = nuevoLibro(1);

        assertThatThrownBy(() -> prestamoService.crear(new PrestamoRequest(lector.getId(), libro.getId())))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("sancionado");
    }

    @Test
    void lectorConAtraso_alPedirOtroPrestamo_quedaSancionado() {
        Usuario lector = nuevoLector();
        PrestamoResponse primero = prestamoService.crear(new PrestamoRequest(lector.getId(), nuevoLibro(1).getId()));
        hacerVencido(primero.id());
        Libro otro = nuevoLibro(1);

        assertThatThrownBy(() -> prestamoService.crear(new PrestamoRequest(lector.getId(), otro.getId())))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("sancionado");

        assertThat(usuarioRepository.findById(lector.getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoUsuario.SANCIONADO);
        assertThat(libroRepository.findById(otro.getId()).orElseThrow().getStockDisponible()).isEqualTo(1);
    }

    @Test
    void devolucion_restauraStock_fechaReal_y_estado() {
        Usuario lector = nuevoLector();
        Libro libro = nuevoLibro(1);
        PrestamoResponse prestamo = prestamoService.crear(new PrestamoRequest(lector.getId(), libro.getId()));
        assertThat(libroRepository.findById(libro.getId()).orElseThrow().getStockDisponible()).isZero();

        PrestamoResponse devuelto = prestamoService.devolver(prestamo.id());

        assertThat(devuelto.estado()).isEqualTo(EstadoPrestamo.DEVUELTO);
        assertThat(devuelto.fechaDevolucionReal()).isEqualTo(LocalDate.now());
        assertThat(libroRepository.findById(libro.getId()).orElseThrow().getStockDisponible()).isEqualTo(1);
    }

    @Test
    void dobleDevolucion_seRechaza_y_noDuplicaStock() {
        Usuario lector = nuevoLector();
        Libro libro = nuevoLibro(1);
        PrestamoResponse prestamo = prestamoService.crear(new PrestamoRequest(lector.getId(), libro.getId()));
        prestamoService.devolver(prestamo.id());

        assertThatThrownBy(() -> prestamoService.devolver(prestamo.id()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("ya fue devuelto");
        assertThat(libroRepository.findById(libro.getId()).orElseThrow().getStockDisponible()).isEqualTo(1);
    }

    @Test
    void prestamoVencido_apareceComoAtrasado() {
        Usuario lector = nuevoLector();
        PrestamoResponse prestamo = prestamoService.crear(new PrestamoRequest(lector.getId(), nuevoLibro(1).getId()));
        hacerVencido(prestamo.id());

        PageResponse<PrestamoResponse> atrasados = prestamoService.atrasados(0, 100);

        assertThat(atrasados.content())
                .anySatisfy(p -> {
                    assertThat(p.id()).isEqualTo(prestamo.id());
                    assertThat(p.estado()).isEqualTo(EstadoPrestamo.ATRASADO);
                });
    }

    @Test
    void devolverUnAtrasado_levantaLaSancion() {
        Usuario lector = nuevoLector();
        PrestamoResponse prestamo = prestamoService.crear(new PrestamoRequest(lector.getId(), nuevoLibro(1).getId()));
        hacerVencido(prestamo.id());
        Libro otro = nuevoLibro(1);
        assertThatThrownBy(() -> prestamoService.crear(new PrestamoRequest(lector.getId(), otro.getId())))
                .isInstanceOf(BusinessRuleException.class);

        prestamoService.devolver(prestamo.id());

        assertThat(usuarioRepository.findById(lector.getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoUsuario.ACTIVO);
    }
}
