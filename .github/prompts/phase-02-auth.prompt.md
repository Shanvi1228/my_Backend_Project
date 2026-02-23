---
agent: agent
description: "Phase 02 – JWT Authentication for both collab-editor and cloud-storage services"
tools: [codebase, editFiles]
---

Reference [copilot-instructions.md](../copilot-instructions.md). Phase 01 must be complete.

## Goal
Full JWT-based authentication (register + login) for BOTH services. Identical pattern, different packages.

## Files to Create in EACH service (collab-editor + cloud-storage)

### Entities
`entity/User.java` — UUID pk, email (unique), username, passwordHash, createdAt, updatedAt
Implements `UserDetails` from Spring Security.

### Repositories
`repository/UserRepository.java`:
```java
Optional<User> findByEmail(String email);
boolean existsByEmail(String email);
```

### DTOs
`dto/request/RegisterRequest.java` — record: email, username, password
`dto/request/LoginRequest.java` — record: email, password
`dto/response/AuthResponse.java` — record: token, userId, username, email

### Security Classes
`security/JwtTokenProvider.java`:
- `generateToken(UUID userId, String email): String`
- `validateToken(String token): boolean`
- `extractUserId(String token): UUID`
- `extractEmail(String token): String`
- Uses jjwt 0.12.x `Jwts.builder()` with `HS512` algorithm
- Secret key loaded from `${app.jwt.secret}` using `@Value`

`security/JwtAuthenticationFilter.java extends OncePerRequestFilter`:
- Reads `Authorization: Bearer <token>` header
- Calls `jwtTokenProvider.validateToken()`
- Sets `UsernamePasswordAuthenticationToken` in `SecurityContextHolder`

`security/UserPrincipal.java implements UserDetails`:
- Wraps `User` entity
- `getId(): UUID`, `getEmail(): String`
- `getAuthorities()` returns empty list (no roles needed for MVP)

`security/CustomUserDetailsService.java implements UserDetailsService`:
- `loadUserByUsername(String email)` — fetches user by email

`config/SecurityConfig.java`:
- Stateless session
- Permit: `POST /api/auth/register`, `POST /api/auth/login`, `/swagger-ui/**`, `/api-docs/**`
- Authenticate all other requests
- Add `JwtAuthenticationFilter` before `UsernamePasswordAuthenticationFilter`
- Add `CorsConfigurationSource` bean allowing localhost:3000 and localhost:3001

### Service
`service/AuthService.java` (interface):
```java
AuthResponse register(RegisterRequest request);
AuthResponse login(LoginRequest request);
```

`service/impl/AuthServiceImpl.java`:
- `register`: check email not taken (throw `ConflictException`), encode password with `BCryptPasswordEncoder`, save user, generate token
- `login`: load user by email (throw `ResourceNotFoundException` if not found), check password (throw `UnauthorizedException` if wrong), generate token

### Controller
`controller/AuthController.java`:
- `POST /api/auth/register` → `authService.register()`
- `POST /api/auth/login` → `authService.login()`
- Both return `ResponseEntity<ApiResponse<AuthResponse>>`
- Add `@Tag(name = "Authentication")` and `@Operation` annotations

### Exception Classes
In `exception/` package, create ALL of these for each service:
- `ApiResponse.java` (record)
- `ResourceNotFoundException.java`
- `UnauthorizedException.java`
- `ConflictException.java`
- `GlobalExceptionHandler.java`

### Config
`config/AppConfig.java` — `@Bean BCryptPasswordEncoder passwordEncoder()`

## Done When
- [ ] `POST /api/auth/register` with `{email, username, password}` returns JWT token
- [ ] `POST /api/auth/login` with `{email, password}` returns JWT token
- [ ] Invalid credentials return 403
- [ ] Duplicate email returns 409
- [ ] Swagger UI at `/swagger-ui.html` shows auth endpoints
- [ ] All authenticated endpoints reject requests without valid JWT with 401
- [ ] Works for BOTH services (ports 8080 and 8081)