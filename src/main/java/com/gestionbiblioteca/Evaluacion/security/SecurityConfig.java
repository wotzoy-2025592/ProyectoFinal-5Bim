package com.gestionbiblioteca.Evaluacion.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String API = "/api/v1";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtService jwtService,
                                                   UserDetailsService userDetailsService,
                                                   RestAuthenticationEntryPoint entryPoint,
                                                   RestAccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                // API stateless con JWT en cabecera: no hay cookies de sesión, por lo que CSRF no aplica.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        // Públicos
                        .requestMatchers(HttpMethod.GET, "/").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.POST, API + "/auth/register", API + "/auth/login").permitAll()
                        // Libros
                        .requestMatchers(HttpMethod.GET, API + "/libros", API + "/libros/*").authenticated()
                        .requestMatchers(HttpMethod.POST, API + "/libros").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, API + "/libros/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, API + "/libros/*").hasRole("ADMIN")
                        // Préstamos
                        .requestMatchers(HttpMethod.POST, API + "/prestamos").hasAnyRole("BIBLIOTECARIO", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, API + "/prestamos/*/devolucion").hasAnyRole("BIBLIOTECARIO", "ADMIN")
                        .requestMatchers(HttpMethod.GET, API + "/prestamos/mis-prestamos").hasRole("LECTOR")
                        .requestMatchers(HttpMethod.GET, API + "/prestamos/atrasados").hasAnyRole("BIBLIOTECARIO", "ADMIN")
                        // Todo lo demás requiere autenticación
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthenticationFilter(jwtService, userDetailsService),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Usa el UserDetailsService + PasswordEncoder definidos como beans (DaoAuthenticationProvider automático). */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
