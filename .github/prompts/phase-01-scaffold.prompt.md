---
agent: agent
description: "Phase 01 – Scaffold monorepo: Maven POMs, DB migrations, Docker Compose, app skeletons"
tools: [codebase, editFiles, runCommands]
---

Reference [copilot-instructions.md](../copilot-instructions.md) for all conventions before starting.

## Goal
Build the complete project skeleton with no business logic. Everything must compile.

## Tasks (complete ALL before stopping)

### Task 1 – Maven Parent POM
Create `backend/pom.xml`:
- groupId: `com.collabstack`, artifactId: `collabstack-parent`, packaging: `pom`
- modules: `collab-editor`, `cloud-storage`
- parent: `spring-boot-starter-parent:3.3.4`
- In `<dependencyManagement>` import: `spring-ai-bom:1.0.0`
- Managed dependency versions: lombok, mapstruct, springdoc-openapi-starter-webmvc-ui:2.5.0, jjwt-api:0.12.6, jjwt-impl:0.12.6, flyway-core, postgresql

### Task 2 – collab-editor module
Create `backend/collab-editor/pom.xml`:
- parent: collabstack-parent
- artifactId: `collab-editor`
- Dependencies to include:
  - spring-boot-starter-web
  - spring-boot-starter-data-jpa
  - spring-boot-starter-security
  - spring-boot-starter-websocket
  - spring-boot-starter-validation
  - spring-ai-openai-spring-boot-starter
  - spring-ai-pgvector-store-spring-boot-starter
  - org.postgresql:postgresql (runtime)
  - flyway-core
  - lombok (provided)
  - mapstruct
  - springdoc-openapi-starter-webmvc-ui
  - jjwt-api + jjwt-impl + jjwt-jackson

Create `backend/collab-editor/src/main/java/com/collabstack/editor/CollabEditorApplication.java`:
```java
@SpringBootApplication
@EnableJpaAuditing
public class CollabEditorApplication {
    public static void main(String[] args) {
        SpringApplication.run(CollabEditorApplication.class, args);
    }
}
```

Create `backend/collab-editor/src/main/resources/application.yml` with:
- server.port: 8080
- spring.application.name: collab-editor
- spring.datasource.url: ${DB_URL:jdbc:postgresql://localhost:5432/collabstack_editor}
- spring.datasource.username: ${DB_USERNAME:collabstack}
- spring.datasource.password: ${DB_PASSWORD:changeme}
- spring.jpa.hibernate.ddl-auto: validate
- spring.flyway.enabled: true, locations: classpath:db/migration
- spring.ai.openai.api-key: ${OPENAI_API_KEY}
- spring.ai.vectorstore.pgvector.dimensions: 1536
- spring.ai.vectorstore.pgvector.index-type: HNSW
- app.jwt.secret: ${JWT_SECRET}
- app.jwt.expiration-ms: 86400000

### Task 3 – cloud-storage module
Same pattern as Task 2.
- artifactId: `cloud-storage`
- server.port: 8081
- DB: collabstack_storage
- No Spring AI dependencies
- Same: web, data-jpa, security, validation, flyway, lombok, mapstruct, springdoc, jjwt, postgresql

Create `CloudStorageApplication.java` with `@SpringBootApplication @EnableJpaAuditing`.

### Task 4 – storage-node module
Create `storage-node/pom.xml` — standalone Spring Boot app (not child of backend parent):
- artifactId: `storage-node`
- Dependencies: spring-boot-starter-web, lombok
- server.port: ${NODE_PORT:9001}

Create `StorageNodeApplication.java`.
Create `storage-node/src/main/resources/application.yml`:
```yaml
server:
  port: ${NODE_PORT:9001}
storage:
  node:
    id: ${NODE_ID:node-1}
    data-dir: ${DATA_DIR:./data/chunks}
```

### Task 5 – DB Migrations (Flyway)
Create `backend/collab-editor/src/main/resources/db/migration/V1__init_schema.sql`:
Use exact DDL from the "collab-editor DB" section in copilot-instructions.md.
Add `CREATE EXTENSION IF NOT EXISTS "pgvector"` and `CREATE EXTENSION IF NOT EXISTS "uuid-ossp"` at the top.

Create `backend/cloud-storage/src/main/resources/db/migration/V1__init_schema.sql`:
Use exact DDL from the "cloud-storage DB" section in copilot-instructions.md.

### Task 6 – Docker Compose
Create `docker-compose.yml`:
```yaml
version: '3.9'
services:
  postgres:
    image: pgvector/pgvector:pg16
    environment:
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./scripts/init-db.sql:/docker-entrypoint-initdb.d/init-db.sql
    ports: ["5432:5432"]

  collab-editor-backend:
    build: ./backend/collab-editor
    ports: ["8080:8080"]
    environment:
      DB_URL: jdbc:postgresql://postgres:5432/collabstack_editor
      DB_USERNAME: ${POSTGRES_USER}
      DB_PASSWORD: ${POSTGRES_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      OPENAI_API_KEY: ${OPENAI_API_KEY}
    depends_on: [postgres]

  cloud-storage-backend:
    build: ./backend/cloud-storage
    ports: ["8081:8081"]
    environment:
      DB_URL: jdbc:postgresql://postgres:5432/collabstack_storage
      DB_USERNAME: ${POSTGRES_USER}
      DB_PASSWORD: ${POSTGRES_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
    depends_on: [postgres]

  storage-node-1:
    build: ./storage-node
    ports: ["9001:9001"]
    environment: { NODE_PORT: 9001, NODE_ID: node-1, DATA_DIR: /data/chunks }
    volumes: ["node1_data:/data/chunks"]

  storage-node-2:
    build: ./storage-node
    ports: ["9002:9001"]
    environment: { NODE_PORT: 9001, NODE_ID: node-2, DATA_DIR: /data/chunks }
    volumes: ["node2_data:/data/chunks"]

  storage-node-3:
    build: ./storage-node
    ports: ["9003:9001"]
    environment: { NODE_PORT: 9001, NODE_ID: node-3, DATA_DIR: /data/chunks }
    volumes: ["node3_data:/data/chunks"]

volumes:
  postgres_data:
  node1_data:
  node2_data:
  node3_data:
```

Create `scripts/init-db.sql`:
```sql
CREATE DATABASE collabstack_editor;
CREATE DATABASE collabstack_storage;
```

### Task 7 – Dockerfiles
Create `backend/collab-editor/Dockerfile`, `backend/cloud-storage/Dockerfile`, `storage-node/Dockerfile` — all using multi-stage build:
```dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Task 8 – .env.example
```
POSTGRES_USER=collabstack
POSTGRES_PASSWORD=changeme_in_production
JWT_SECRET=change_this_to_64_plus_char_random_string_before_use
OPENAI_API_KEY=sk-replace-with-real-key
```

### Task 9 – Frontend Scaffolds
Generate `frontend/collab-editor/package.json` (Vite React-TS):
Dependencies: react, react-dom, react-router-dom, axios, zustand, @tanstack/react-query, @tiptap/react, @tiptap/pm, @tiptap/starter-kit, @tiptap/extension-collaboration, @tiptap/extension-collaboration-cursor
DevDependencies: vite, typescript, tailwindcss, autoprefixer, postcss, @types/react, @types/react-dom

Generate `frontend/cloud-storage/package.json` (same minus TipTap).

Create `frontend/collab-editor/src/main.tsx`, `App.tsx` (with QueryClientProvider + Router), `index.css` (Tailwind directives).
Create `frontend/cloud-storage/src/main.tsx`, `App.tsx`, `index.css`.

## Done When
- [ ] `mvn compile` passes for both backend modules
- [ ] `docker-compose up postgres` starts cleanly and creates both DBs
- [ ] Both `application.yml` files exist with env-var placeholders
- [ ] Both DB migration SQL files exist with all tables from the schema
- [ ] Both frontend `package.json` files exist
- [ ] `.env.example` exists