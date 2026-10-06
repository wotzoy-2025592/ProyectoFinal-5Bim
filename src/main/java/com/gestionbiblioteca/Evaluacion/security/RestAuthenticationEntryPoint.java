package com.gestionbiblioteca.Evaluacion.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** 401: sin token, token inválido o expirado. */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        JsonErrorWriter.write(response, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED",
                "Autenticación requerida: token ausente, inválido o expirado", request.getRequestURI());
    }
}
