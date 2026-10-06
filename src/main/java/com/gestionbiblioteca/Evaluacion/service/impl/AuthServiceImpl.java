package com.gestionbiblioteca.Evaluacion.service.impl;

import com.gestionbiblioteca.Evaluacion.dto.AuthResponse;
import com.gestionbiblioteca.Evaluacion.dto.LoginRequest;
import com.gestionbiblioteca.Evaluacion.dto.RegisterRequest;
import com.gestionbiblioteca.Evaluacion.dto.UsuarioResponse;
import com.gestionbiblioteca.Evaluacion.entity.EstadoUsuario;
import com.gestionbiblioteca.Evaluacion.entity.Rol;
import com.gestionbiblioteca.Evaluacion.entity.Usuario;
import com.gestionbiblioteca.Evaluacion.exception.DuplicateResourceException;
import com.gestionbiblioteca.Evaluacion.repository.UsuarioRepository;
import com.gestionbiblioteca.Evaluacion.security.JwtService;
import com.gestionbiblioteca.Evaluacion.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Override
    @Transactional
    public UsuarioResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (usuarioRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Ya existe un usuario registrado con el email " + email);
        }
        // Registro público: SIEMPRE LECTOR y ACTIVO (el rol no viene del cliente)
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nombre(request.nombre().trim())
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .rol(Rol.LECTOR)
                .estado(EstadoUsuario.ACTIVO)
                .build());
        return new UsuarioResponse(usuario.getId(), usuario.getNombre(), usuario.getEmail(),
                usuario.getEstado(), usuario.getRol());
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        // Lanza BadCredentialsException si las credenciales no son válidas
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));
        String token = jwtService.generateToken(usuario.getEmail(), usuario.getRol());
        return new AuthResponse(token, "Bearer", jwtService.getExpirationMs(), usuario.getId(),
                usuario.getNombre(), usuario.getEmail(), usuario.getRol());
    }
}
