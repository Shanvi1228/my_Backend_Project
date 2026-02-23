---
applyTo: "backend/**,storage-node/**"
---

# Backend-Specific Instructions

## Spring Boot Config Checklist
Every Spring Boot application.yml must include:
```yaml
spring:
  application:
    name: ${SERVICE_NAME}
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate.format_sql: true
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
app:
  jwt:
    secret: ${JWT_SECRET}
    expiration-ms: 86400000
springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
```

## JWT Implementation
Generate these exact classes in the `security/` package:
- `JwtTokenProvider` — generate, validate, extract userId from token
- `JwtAuthenticationFilter extends OncePerRequestFilter` — read Bearer token, set SecurityContext
- `SecurityConfig extends WebSecurityConfigurerAdapter` — permit auth endpoints, secure rest
- `UserPrincipal implements UserDetails` — wraps User entity for Spring Security
- `CustomUserDetailsService implements UserDetailsService` — loads by email

JwtTokenProvider must expose:
```java
public String generateToken(UUID userId, String email)
public boolean validateToken(String token)
public UUID extractUserId(String token)
```

## WebSocket Config (collab-editor only)
```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
    }
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
    }
}
```

## Controller Pattern with Swagger
```java
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Tag(name = "Documents", description = "Document management APIs")
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping
    @Operation(summary = "Create a new document")
    public ResponseEntity<ApiResponse<DocumentResponse>> create(
            @RequestBody @Valid DocumentCreateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        DocumentResponse response = documentService.create(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Document created", response));
    }
}
```

## GlobalExceptionHandler Template
```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(UnauthorizedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(ConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("Internal server error"));
    }
}
```

## MapStruct Mapper Pattern
```java
@Mapper(componentModel = "spring")
public interface DocumentMapper {
    DocumentResponse toResponse(Document document);
    Document toEntity(DocumentCreateRequest request);
    List<DocumentResponse> toResponseList(List<Document> documents);
}
```

## Service Validation Rules
- Always throw `ResourceNotFoundException` when `.findById()` returns empty
- Always verify ownership: if resource belongs to another user → throw `UnauthorizedException`
- Log entry and exit of important service methods with `log.info()`
- Use `@Transactional` on all write methods in ServiceImpl