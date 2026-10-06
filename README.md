# Gestor de Biblioteca — API REST

Backend de una biblioteca: autenticación JWT stateless, roles, catálogo, préstamos, devoluciones, control de stock, atrasos y sanciones.

## Tecnologías
Java 21 · Spring Boot 4.1.1 · Spring Web · Spring Security (JWT + BCrypt) · Spring Data JPA / Hibernate · PostgreSQL · Lombok · Thymeleaf (solo estructura mínima) · JJWT · JUnit 5 / MockMvc (tests con H2 en memoria).

Package raíz: `com.gestionbiblioteca.Evaluacion`

## Estructura
```
config/       DataInitializer (datos iniciales), SchedulingConfig, PrestamoScheduler
controller/   AuthController, LibroController, PrestamoController, HomeController
dto/          Requests/Responses (records), PageResponse, ErrorResponse
entity/       Usuario, Libro, Prestamo, Rol, EstadoUsuario, EstadoPrestamo
exception/    ResourceNotFound, BusinessRule, DuplicateResource, GlobalExceptionHandler
mapper/       LibroMapper, PrestamoMapper
repository/   Spring Data JPA (con bloqueos pesimistas)
security/     SecurityConfig, JwtService, JwtAuthenticationFilter, CustomUserDetailsService, handlers 401/403
service/      interfaces  ·  service/impl/ implementaciones (toda la lógica de negocio)
```
Capas: `Controller → Service → Repository → JPA/Hibernate → PostgreSQL`.

## Dependencias adicionales (agregar al pom.xml existente)
Solo estas; el resto ya viene de Spring Initializr. Las versiones de Spring/Hibernate/H2 las gestiona Spring Boot.

```xml
<!-- Jakarta Validation (@NotBlank, @Email, ...) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

<!-- JWT (JJWT) -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.13.0</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.13.0</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.13.0</version>
    <scope>runtime</scope>
</dependency>

<!-- Solo tests: base de datos en memoria -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

## Configuración
1. Elimina `src/main/resources/application.properties` (el generado por Initializr) y usa `application.yml`.
2. Crea la base de datos:
   ```sql
   CREATE DATABASE biblioteca;
   ```
   Hibernate crea las tablas automáticamente (`ddl-auto: update`).
3. Variables de entorno:

| Variable | Descripción | Ejemplo |
|---|---|---|
| `DB_URL` | JDBC (opcional, por defecto `jdbc:postgresql://localhost:5432/biblioteca`) | `jdbc:postgresql://localhost:5432/biblioteca` |
| `DB_USERNAME` | Usuario de PostgreSQL | `postgres` |
| `DB_PASSWORD` | Contraseña de PostgreSQL | *(la tuya)* |
| `JWT_SECRET` | Clave de firma, **mínimo 32 caracteres** | *(cadena aleatoria larga)* |
| `JWT_EXPIRATION` | Vida del token en ms (opcional, por defecto 3600000) | `3600000` |

Linux/macOS:
```bash
export DB_USERNAME=postgres DB_PASSWORD=tu_password
export JWT_SECRET="cambia-esto-por-una-clave-larga-de-32-o-mas-caracteres"
```
Windows PowerShell:
```powershell
$env:DB_USERNAME="postgres"; $env:DB_PASSWORD="tu_password"
$env:JWT_SECRET="cambia-esto-por-una-clave-larga-de-32-o-mas-caracteres"
```
En IntelliJ: *Run → Edit Configurations → Environment variables*.

## Ejecución
```bash
./mvnw spring-boot:run      # Windows: mvnw.cmd spring-boot:run
```
La API queda en `http://localhost:8080`.

## Usuarios de prueba (solo desarrollo)
| Rol | Email | Contraseña |
|---|---|---|
| ADMIN | admin@biblioteca.com | Admin123! |
| BIBLIOTECARIO | bibliotecario@biblioteca.com | Bibliotecario123! |
| LECTOR | lector@biblioteca.com | Lector123! |

Se crean al arrancar si no existen (BCrypt), junto con 8 libros de ejemplo. El libro "1984" tiene stock 1 (útil para probar "sin stock" y concurrencia).

## Roles
- **ADMIN**: catálogo completo (crear/editar/eliminar), préstamos, devoluciones, atrasados.
- **BIBLIOTECARIO**: registrar préstamos y devoluciones, consultar atrasados, consultar catálogo.
- **LECTOR**: consultar catálogo y sus propios préstamos (historial y activos).

## Endpoints
| Método | URL | Acceso |
|---|---|---|
| POST | `/api/v1/auth/register` | Público (siempre crea LECTOR/ACTIVO) |
| POST | `/api/v1/auth/login` | Público |
| GET | `/api/v1/libros?titulo=&categoria=&page=0&size=10` | Autenticado |
| GET | `/api/v1/libros/{id}` | Autenticado |
| POST | `/api/v1/libros` | ADMIN |
| PUT | `/api/v1/libros/{id}` | ADMIN |
| DELETE | `/api/v1/libros/{id}` | ADMIN |
| POST | `/api/v1/prestamos` | BIBLIOTECARIO, ADMIN |
| PATCH | `/api/v1/prestamos/{id}/devolucion` | BIBLIOTECARIO, ADMIN |
| GET | `/api/v1/prestamos/mis-prestamos?estado=&page=&size=` | LECTOR |
| GET | `/api/v1/prestamos/atrasados?page=&size=` | BIBLIOTECARIO, ADMIN |

Errores con formato uniforme:
```json
{ "timestamp": "2026-10-06T12:00:00", "status": 400, "error": "BUSINESS_RULE",
  "message": "El usuario ya tiene 3 préstamos activos", "path": "/api/v1/prestamos" }
```
Códigos: 400 `BUSINESS_RULE` / `VALIDATION_ERROR`, 401 `UNAUTHORIZED`, 403 `FORBIDDEN`, 404 `NOT_FOUND`, 409 `CONFLICT`.

## JWT
`POST /api/v1/auth/login` devuelve `token`. Se envía en cada petición: `Authorization: Bearer <token>`.
Sin sesiones (`STATELESS`), CSRF deshabilitado (API sin cookies), el token no se guarda en base de datos. `JwtAuthenticationFilter` (OncePerRequestFilter) valida el token y puebla el `SecurityContext`.

## Reglas de negocio
1. **Stock**: no se presta con `stockDisponible == 0`; prestar `-1`, devolver `+1`; nunca negativo.
2. **Máximo 3 préstamos activos** por LECTOR (cuentan ACTIVO y ATRASADO).
3. **Plazo 14 días**: `fechaDevolucionEsperada = fechaPrestamo + 14`.
4. **Atrasos**: si hoy > `fechaDevolucionEsperada` y no se devolvió → `ATRASADO` (se actualiza al consultar/prestar/devolver y cada hora vía scheduler).
5. **Sanción**: un LECTOR con préstamo atrasado que pide otro queda `SANCIONADO` y el préstamo se rechaza. Al devolver su último atrasado vuelve a `ACTIVO`.
6. **Devolución**: fecha real = hoy, estado `DEVUELTO`, stock `+1`; no se puede devolver dos veces.

Los préstamos solo se registran a usuarios con rol LECTOR.

## Concurrencia
Si `stockDisponible = 1` y dos bibliotecarios prestan a la vez, se usa **bloqueo pesimista** (`@Lock(PESSIMISTIC_WRITE)` → `SELECT ... FOR UPDATE`) sobre usuario y libro dentro de la transacción: la segunda petición espera, ve `stock = 0` y es rechazada. Los bloqueos se toman siempre en el mismo orden (usuario → libro) para evitar deadlocks. Se eligió pesimista porque el conflicto es probable en libros populares y evita reintentos.

## Simular un préstamo atrasado (desarrollo)
Crea un préstamo y luego, en PostgreSQL:
```sql
UPDATE prestamos
SET fecha_prestamo = CURRENT_DATE - 20,
    fecha_devolucion_esperada = CURRENT_DATE - 6
WHERE id = <ID_DEL_PRESTAMO>;
```
`GET /api/v1/prestamos/atrasados` lo marcará `ATRASADO`; si ese lector pide otro préstamo quedará `SANCIONADO`.

## Postman
Importa `postman/GestorBiblioteca.postman_collection.json`. Variables: `baseUrl` (por defecto `http://localhost:8080`) y `token`. Los requests de login guardan el JWT automáticamente y cada request usa `Authorization: Bearer {{token}}` con el token del rol que le corresponde. Ejecuta la colección en orden con el Collection Runner:
REGISTER · LOGIN ADMIN · LOGIN BIBLIOTECARIO · LOGIN LECTOR · LIST LIBROS · GET LIBRO · CREATE LIBRO · UPDATE LIBRO · DELETE LIBRO · CREATE PRESTAMO · MIS PRESTAMOS · PRESTAMOS ATRASADOS · DEVOLUCION.

## Pruebas
```bash
./mvnw test
```
Usan H2 en memoria (`src/test/resources/application.yml`), no necesitan PostgreSQL. Cubren seguridad (login, sin JWT, JWT inválido, roles), libros (CRUD, filtros, permisos) y préstamos (correcto, sin stock, cuarto préstamo, sancionado, devolución, doble devolución, atrasado).
`EvaluacionApplicationTests.java` reemplaza al test generado por Initializr para compartir el mismo contexto.
