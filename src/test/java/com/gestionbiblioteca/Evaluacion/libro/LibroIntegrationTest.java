package com.gestionbiblioteca.Evaluacion.libro;

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

class LibroIntegrationTest extends BaseIntegrationTest {

    private static String libroJson(String isbn, String titulo, String categoria, int stock) {
        return """
                {"isbn":"%s","titulo":"%s","autor":"Autor Test","categoria":"%s","stockTotal":%d}
                """.formatted(isbn, titulo, categoria, stock);
    }

    private static String isbnUnico() {
        return "T-" + UUID.randomUUID().toString().substring(0, 12);
    }

    private long crearComoAdmin(String isbn, String titulo, String categoria, int stock) throws Exception {
        String body = mockMvc.perform(post("/api/v1/libros")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(libroJson(isbn, titulo, categoria, stock)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return toLong(JsonPath.read(body, "$.id"));
    }

    @Test
    void crearLibroComoAdmin_201_conStockDisponibleIgualAlTotal() throws Exception {
        mockMvc.perform(post("/api/v1/libros")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(libroJson(isbnUnico(), "Libro Admin", "Test", 3)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.stockTotal").value(3))
                .andExpect(jsonPath("$.stockDisponible").value(3));
    }

    @Test
    void crearLibroComoLector_403() throws Exception {
        mockMvc.perform(post("/api/v1/libros")
                        .header(HttpHeaders.AUTHORIZATION, bearer(lectorToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(libroJson(isbnUnico(), "No debe crearse", "Test", 1)))
                .andExpect(status().isForbidden());
    }

    @Test
    void crearLibroInvalido_400_validacion() throws Exception {
        mockMvc.perform(post("/api/v1/libros")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isbn\":\"\",\"titulo\":\"\",\"autor\":\"a\",\"categoria\":\"c\",\"stockTotal\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void crearLibroConIsbnDuplicado_409() throws Exception {
        String isbn = isbnUnico();
        crearComoAdmin(isbn, "Original", "Test", 1);
        mockMvc.perform(post("/api/v1/libros")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(libroJson(isbn, "Copia", "Test", 1)))
                .andExpect(status().isConflict());
    }

    @Test
    void consultarLibros_conFiltroYPaginacion() throws Exception {
        String categoria = "Cat-" + UUID.randomUUID().toString().substring(0, 8);
        crearComoAdmin(isbnUnico(), "Alfa Filtro", categoria, 2);
        crearComoAdmin(isbnUnico(), "Beta Filtro", categoria, 2);

        String token = lectorToken();
        mockMvc.perform(get("/api/v1/libros")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .param("categoria", categoria)
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content.length()").value(1));

        mockMvc.perform(get("/api/v1/libros")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .param("categoria", categoria)
                        .param("titulo", "alfa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].titulo").value("Alfa Filtro"));
    }

    @Test
    void consultarLibroPorId_yNoExistente() throws Exception {
        long id = crearComoAdmin(isbnUnico(), "Detalle", "Test", 1);
        String token = lectorToken();

        mockMvc.perform(get("/api/v1/libros/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Detalle"));

        mockMvc.perform(get("/api/v1/libros/{id}", 999999999L).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void actualizarLibroComoAdmin() throws Exception {
        String isbn = isbnUnico();
        long id = crearComoAdmin(isbn, "Antes", "Test", 2);

        mockMvc.perform(put("/api/v1/libros/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(libroJson(isbn, "Despues", "Test", 5)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Despues"))
                .andExpect(jsonPath("$.stockTotal").value(5))
                .andExpect(jsonPath("$.stockDisponible").value(5));
    }

    @Test
    void eliminarLibroComoAdmin_yLuegoNoExiste() throws Exception {
        long id = crearComoAdmin(isbnUnico(), "Borrar", "Test", 1);

        mockMvc.perform(delete("/api/v1/libros/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/libros/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isNotFound());
    }
}
