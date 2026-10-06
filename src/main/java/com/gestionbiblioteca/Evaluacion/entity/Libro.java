package com.gestionbiblioteca.Evaluacion.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "libros")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Libro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String isbn;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(nullable = false, length = 150)
    private String autor;

    @Column(nullable = false, length = 100)
    private String categoria;

    @Column(name = "stock_total", nullable = false)
    private int stockTotal;

    @Column(name = "stock_disponible", nullable = false)
    private int stockDisponible;
}
