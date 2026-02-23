---
agent: agent
description: "Phase 03 – Document CRUD REST API in collab-editor service"
tools: [codebase, editFiles]
---

Reference [copilot-instructions.md](../copilot-instructions.md). Phase 02 must be complete.

## Goal
Full REST CRUD for Documents with collaborator management. Only in `collab-editor` service.

## Files to Create

### Entity
`entity/Document.java`:
- UUID id, User owner (ManyToOne), String title, String contentSnapshot, Long currentRevision (default 0), Set<DocumentCollaborator> collaborators

`entity/DocumentCollaborator.java`:
- UUID id, Document document, User user, CollaboratorRole role (enum: VIEWER, EDITOR)
- Unique constraint on (document_id, user_id)

`entity/CollaboratorRole.java` — enum: VIEWER, EDITOR

### DTOs
`dto/request/DocumentCreateRequest.java` — record: title, initialContent (nullable)
`dto/request/AddCollaboratorRequest.java` — record: email, role
`dto/response/DocumentResponse.java` — record: id, title, contentSnapshot, currentRevision, ownerUsername, collaboratorCount, createdAt, updatedAt
`dto/response/CollaboratorResponse.java` — record: userId, username, email, role

### Repository
`repository/DocumentRepository.java`:
```java
List<Document> findAllByOwnerIdOrCollaborators_UserId(UUID ownerId, UUID collaboratorUserId);
boolean existsByIdAndOwnerId(UUID id, UUID ownerId);
```

`repository/DocumentCollaboratorRepository.java`:
```java
Optional<DocumentCollaborator> findByDocumentIdAndUserId(UUID docId, UUID userId);
List<DocumentCollaborator> findAllByDocumentId(UUID docId);
```

### Service
`service/DocumentService.java` interface:
```java
DocumentResponse create(UUID userId, DocumentCreateRequest request);
DocumentResponse findById(UUID documentId, UUID requestingUserId);
List<DocumentResponse> findAllForUser(UUID userId);
DocumentResponse updateTitle(UUID documentId, UUID userId, String newTitle);
void delete(UUID documentId, UUID userId);
CollaboratorResponse addCollaborator(UUID documentId, UUID ownerUserId, AddCollaboratorRequest request);
List<CollaboratorResponse> getCollaborators(UUID documentId, UUID userId);
```

`service/impl/DocumentServiceImpl.java`:
- `create`: save Document with owner, empty contentSnapshot, revision=0
- `findById`: check user is owner OR collaborator — throw `UnauthorizedException` if neither
- `findAllForUser`: query by ownerId OR as collaborator
- `updateTitle`: owner-only operation
- `delete`: owner-only, cascades to collaborators + operations
- `addCollaborator`: owner finds user by email, creates DocumentCollaborator

### Controller
`controller/DocumentController.java`:
```
GET    /api/documents          → findAllForUser
POST   /api/documents          → create
GET    /api/documents/{id}     → findById
PUT    /api/documents/{id}/title → updateTitle
DELETE /api/documents/{id}     → delete
POST   /api/documents/{id}/collaborators  → addCollaborator
GET    /api/documents/{id}/collaborators  → getCollaborators
```
All endpoints require JWT. Extract userId from `@AuthenticationPrincipal UserPrincipal`.

### Mapper
`mapper/DocumentMapper.java` (MapStruct):
- `toResponse(Document)`, `toResponseList(List<Document>)`
- `toCollaboratorResponse(DocumentCollaborator)`

## Done When
- [ ] `POST /api/documents` creates a document, returns DocumentResponse
- [ ] `GET /api/documents` lists only docs for the authenticated user
- [ ] Non-collaborator trying to GET a doc gets 403
- [ ] `POST /api/documents/{id}/collaborators` with `{email, role}` adds collaborator
- [ ] Delete cascades correctly
- [ ] All endpoints visible in Swagger UI