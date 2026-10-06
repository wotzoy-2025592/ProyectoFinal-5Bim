package com.gestionbiblioteca.Evaluacion.security;

import com.gestionbiblioteca.Evaluacion.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SeguridadIntegrationTest extends BaseIntegrationTest {

    @Test
    void loginCorrecto_devuelveJwtSinPassword() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + ADMIN_EMAIL + "\",\"password\":\"" + ADMIN_PASS + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.rol").value("ADMIN"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void loginIncorrecto_devuelve401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + ADMIN_EMAIL + "\",\"password\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    void endpointProtegidoSinJwt_devuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/libros"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void jwtInvalido_devuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/libros")
                        .header(HttpHeaders.AUTHORIZATION, bearer("esto.no.es-un-jwt")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void lector_noPuedeCrearLibro_403() throws Exception {
        mockMvc.perform(post("/api/v1/libros")
                        .header(HttpHeaders.AUTHORIZATION, bearer(lectorToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isbn\":\"X-1\",\"titulo\":\"t\",\"autor\":\"a\",\"categoria\":\"c\",\"stockTotal\":1}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void lector_noPuedeVerAtrasados_ni_crearPrestamos_403() throws Exception {
        String token = lectorToken();
        mockMvc.perform(get("/api/v1/prestamos/atrasados")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/prestamos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":1,\"libroId\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void bibliotecario_noPuedeCrearLibro_pero_puedeVerAtrasados() throws Exception {
        String token = bibliotecarioToken();
        mockMvc.perform(post("/api/v1/libros")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isbn\":\"X-2\",\"titulo\":\"t\",\"autor\":\"a\",\"categoria\":\"c\",\"stockTotal\":1}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/prestamos/atrasados")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    void admin_noPuedeUsarMisPrestamos_soloLector_403() throws Exception {
        mockMvc.perform(get("/api/v1/prestamos/mis-prestamos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isForbidden());
    }

    @Test
    void registroPublico_asignaLectorActivo() throws Exception {
        String email = "nuevo-" + UUID.randomUUID() + "@test.com";
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Nuevo\",\"email\":\"" + email + "\",\"password\":\"123456\",\"rol\":\"ADMIN\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("LECTOR"))
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void registroConEmailDuplicado_devuelve409() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Dup\",\"email\":\"" + LECTOR_EMAIL + "\",\"password\":\"123456\"}"))
                .andExpect(status().isConflict());
    }
}
