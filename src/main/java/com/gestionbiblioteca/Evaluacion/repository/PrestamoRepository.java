package com.gestionbiblioteca.Evaluacion.repository;

import com.gestionbiblioteca.Evaluacion.entity.EstadoPrestamo;
import com.gestionbiblioteca.Evaluacion.entity.Prestamo;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;

public interface PrestamoRepository extends JpaRepository<Prestamo, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Prestamo p where p.id = :id")
    Optional<Prestamo> findByIdForUpdate(@Param("id") Long id);

    long countByUsuarioIdAndEstadoIn(Long usuarioId, Collection<EstadoPrestamo> estados);

    boolean existsByUsuarioIdAndEstado(Long usuarioId, EstadoPrestamo estado);

    boolean existsByLibroId(Long libroId);

    @EntityGraph(attributePaths = {"usuario", "libro"})
    Page<Prestamo> findByUsuarioId(Long usuarioId, Pageable pageable);

    @EntityGraph(attributePaths = {"usuario", "libro"})
    Page<Prestamo> findByUsuarioIdAndEstado(Long usuarioId, EstadoPrestamo estado, Pageable pageable);

    @EntityGraph(attributePaths = {"usuario", "libro"})
    Page<Prestamo> findByEstado(EstadoPrestamo estado, Pageable pageable);

    /** Marca como ATRASADO los préstamos vencidos y no devueltos. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Prestamo p set p.estado = :nuevo "
            + "where p.estado = :actual and p.fechaDevolucionEsperada < :hoy")
    int actualizarEstadoVencidos(@Param("nuevo") EstadoPrestamo nuevo,
                                 @Param("actual") EstadoPrestamo actual,
                                 @Param("hoy") LocalDate hoy);
}
