package com.gestionbiblioteca.Evaluacion.prestamo;

import com.gestionbiblioteca.Evaluacion.BaseIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Flujo HTTP completo con JWT y roles. Usa un lector recién registrado para no interferir con otros tests. */
class PrestamoControllerIntegrationTest extends BaseIntegrationTest {

    @Test
    void flujoCompleto_crear_misPrestamos_atrasados_devolver_dobleDevolucion() throws Exception {
        // ADMIN crea un libro
        String isbn = "T-" + UUID.randomUUID().toString().substring(0, 12);
        String libroBody = mockMvc.perform(post("/api/v1/libros")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isbn\":\"" + isbn + "\",\"titulo\":\"Flujo HTTP\",\"autor\":\"A\",\"categoria\":\"Test\",\"stockTotal\":2}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        long libroId = toLong(JsonPath.read(libroBody, "$.id"));

        // Se registra un lector nuevo y se obtiene su token
        String email = "flujo-" + UUID.randomUUID() + "@test.com";
        String regBody = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Flujo\",\"email\":\"" + email + "\",\"password\":\"123456\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        long lectorId = toLong(JsonPath.read(regBody, "$.id"));
        String lectorToken = login(email, "123456");
        String biblioToken = bibliotecarioToken();

        // BIBLIOTECARIO registra el préstamo
        String prestamoBody = mockMvc.perform(post("/api/v1/prestamos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(biblioToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":" + lectorId + ",\"libroId\":" + libroId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andExpect(jsonPath("$.libroId").value(libroId))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        long prestamoId = toLong(JsonPath.read(prestamoBody, "$.id"));

        // El stock disminuyó
        mockMvc.perform(get("/api/v1/libros/{id}", libroId).header(HttpHeaders.AUTHORIZATION, bearer(lectorToken)))
                .andExpect(jsonPath("$.stockDisponible").value(1));

        // LECTOR ve solo sus préstamos
        mockMvc.perform(get("/api/v1/prestamos/mis-prestamos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(lectorToken))
                        .param("estado", "ACTIVO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].usuarioId").value(lectorId));

        // BIBLIOTECARIO consulta atrasados
        mockMvc.perform(get("/api/v1/prestamos/atrasados").header(HttpHeaders.AUTHORIZATION, bearer(biblioToken)))
                .andExpect(status().isOk());

        // Devolución
        mockMvc.perform(patch("/api/v1/prestamos/{id}/devolucion", prestamoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(biblioToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("DEVUELTO"))
                .andExpect(jsonPath("$.fechaDevolucionReal").isNotEmpty());

        mockMvc.perform(get("/api/v1/libros/{id}", libroId).header(HttpHeaders.AUTHORIZATION, bearer(lectorToken)))
                .andExpect(jsonPath("$.stockDisponible").value(2));

        // Segunda devolución: regla de negocio
        mockMvc.perform(patch("/api/v1/prestamos/{id}/devolucion", prestamoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(biblioToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BUSINESS_RULE"));
    }

    @Test
    void lector_noPuedeRegistrarPrestamos_403() throws Exception {
        mockMvc.perform(post("/api/v1/prestamos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(lectorToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":1,\"libroId\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void prestamoConLibroInexistente_404() throws Exception {
        mockMvc.perform(post("/api/v1/prestamos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(bibliotecarioToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":999999999,\"libroId\":999999999}"))
                .andExpect(status().isNotFound());
    }
}
