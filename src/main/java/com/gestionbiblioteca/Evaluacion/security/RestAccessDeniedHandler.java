package com.gestionbiblioteca.Evaluacion.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** 403: autenticado pero sin el rol necesario. */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        JsonErrorWriter.write(response, HttpServletResponse.SC_FORBIDDEN, "FORBIDDEN",
                "No tiene permisos para realizar esta operación", request.getRequestURI());
    }
}
