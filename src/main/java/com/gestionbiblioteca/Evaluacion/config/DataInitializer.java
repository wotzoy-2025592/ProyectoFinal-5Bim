package com.gestionbiblioteca.Evaluacion.config;

import com.gestionbiblioteca.Evaluacion.entity.EstadoUsuario;
import com.gestionbiblioteca.Evaluacion.entity.Libro;
import com.gestionbiblioteca.Evaluacion.entity.Rol;
import com.gestionbiblioteca.Evaluacion.entity.Usuario;
import com.gestionbiblioteca.Evaluacion.repository.LibroRepository;
import com.gestionbiblioteca.Evaluacion.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/** Carga inicial idempotente: credenciales de DESARROLLO/PRUEBA y catálogo de ejemplo. */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final LibroRepository libroRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        crearUsuarioSiNoExiste("Administrador", "admin@biblioteca.com", "Admin123*", Rol.ADMIN);
        crearUsuarioSiNoExiste("Bibliotecario", "bibliotecario@biblioteca.com", "Bibliotecario123*", Rol.BIBLIOTECARIO);
        crearUsuarioSiNoExiste("Lector de Prueba", "lector@biblioteca.com", "Lector123*", Rol.LECTOR);

        if (libroRepository.count() == 0) {
            libroRepository.saveAll(List.of(
                    libro("978-1-00000-001-1", "Cien años de soledad", "Gabriel García Márquez", "Novela", 5),
                    libro("978-1-00000-002-2", "Don Quijote de la Mancha", "Miguel de Cervantes", "Clásicos", 4),
                    libro("978-1-00000-003-3", "Clean Code", "Robert C. Martin", "Tecnología", 3),
                    libro("978-1-00000-004-4", "Effective Java", "Joshua Bloch", "Tecnología", 3),
                    libro("978-1-00000-005-5", "Sapiens", "Yuval Noah Harari", "Historia", 6),
                    libro("978-1-00000-006-6", "El Principito", "Antoine de Saint-Exupéry", "Infantil", 5),
                    libro("978-1-00000-007-7", "Rayuela", "Julio Cortázar", "Novela", 2),
                    // Stock 1: útil para probar "sin stock" y la concurrencia
                    libro("978-1-00000-008-8", "1984", "George Orwell", "Ciencia ficción", 1)
            ));
            log.info("Catálogo inicial de libros cargado");
        }
    }

    private void crearUsuarioSiNoExiste(String nombre, String email, String password, Rol rol) {
        if (!usuarioRepository.existsByEmail(email)) {
            usuarioRepository.save(Usuario.builder()
                    .nombre(nombre)
                    .email(email)
                    .password(passwordEncoder.encode(password))
                    .rol(rol)
                    .estado(EstadoUsuario.ACTIVO)
                    .build());
            log.info("Usuario de prueba creado: {} ({})", email, rol);
        }
    }

    private Libro libro(String isbn, String titulo, String autor, String categoria, int stock) {
        return Libro.builder().isbn(isbn).titulo(titulo).autor(autor).categoria(categoria)
                .stockTotal(stock).stockDisponible(stock).build();
    }
}
