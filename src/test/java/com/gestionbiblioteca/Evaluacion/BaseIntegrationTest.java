package com.gestionbiblioteca.Evaluacion;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Base común: un único contexto de Spring compartido por todos los tests (H2 en memoria). */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class BaseIntegrationTest {

    protected static final String ADMIN_EMAIL = "admin@biblioteca.com";
    protected static final String ADMIN_PASS = "Admin123!";
    protected static final String BIBLIO_EMAIL = "bibliotecario@biblioteca.com";
    protected static final String BIBLIO_PASS = "Bibliotecario123!";
    protected static final String LECTOR_EMAIL = "lector@biblioteca.com";
    protected static final String LECTOR_PASS = "Lector123!";

    @Autowired
    protected MockMvc mockMvc;

    protected String login(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(body, "$.token");
    }

    protected String adminToken() throws Exception {
        return login(ADMIN_EMAIL, ADMIN_PASS);
    }

    protected String bibliotecarioToken() throws Exception {
        return login(BIBLIO_EMAIL, BIBLIO_PASS);
    }

    protected String lectorToken() throws Exception {
        return login(LECTOR_EMAIL, LECTOR_PASS);
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }

    protected static long toLong(Object number) {
        return ((Number) number).longValue();
    }
}
