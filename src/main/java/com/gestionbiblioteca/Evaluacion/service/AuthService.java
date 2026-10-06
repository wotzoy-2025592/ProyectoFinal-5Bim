package com.gestionbiblioteca.Evaluacion.service;

import com.gestionbiblioteca.Evaluacion.dto.AuthResponse;
import com.gestionbiblioteca.Evaluacion.dto.LoginRequest;
import com.gestionbiblioteca.Evaluacion.dto.RegisterRequest;
import com.gestionbiblioteca.Evaluacion.dto.UsuarioResponse;

public interface AuthService {

    UsuarioResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}
