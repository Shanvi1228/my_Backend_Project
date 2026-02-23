package com.collabstack.storage.controller;

import com.collabstack.storage.dto.request.RegisterNodeRequest;
import com.collabstack.storage.dto.response.FileMetadataResponse;
import com.collabstack.storage.dto.response.StorageNodeResponse;
import com.collabstack.storage.dto.response.UploadResponse;
import com.collabstack.storage.exception.ApiResponse;
import com.collabstack.storage.security.UserPrincipal;
import com.collabstack.storage.service.FileStorageService;
import com.collabstack.storage.service.FileUploadService;
import com.collabstack.storage.service.ReplicationRepairService;
import com.collabstack.storage.service.StorageNodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class FileController {

    private final FileUploadService fileUploadService;
    private final FileStorageService fileStorageService;
    private final StorageNodeService storageNodeService;
    private final ReplicationRepairService replicationRepairService;

    // ── File endpoints ────────────────────────────────────────────────────────

    @PostMapping(value = "/api/files/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Tag(name = "Files")
    @Operation(summary = "Upload and encrypt a file")
    public ResponseEntity<ApiResponse<UploadResponse>> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam("password") String userPassword,
            @AuthenticationPrincipal UserPrincipal principal) {
        UploadResponse response = fileUploadService.upload(principal.getId(), file, userPassword);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("File uploaded", response));
    }

    @GetMapping("/api/files")
    @Tag(name = "Files")
    @Operation(summary = "List all files for the authenticated user")
    public ResponseEntity<ApiResponse<List<FileMetadataResponse>>> listFiles(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<FileMetadataResponse> files = fileStorageService.listFiles(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(files));
    }

    @GetMapping("/api/files/{fileId}")
    @Tag(name = "Files")
    @Operation(summary = "Download and decrypt a file")
    public ResponseEntity<byte[]> downloadFile(
            @PathVariable UUID fileId,
            @RequestParam("password") String userPassword,
            @AuthenticationPrincipal UserPrincipal principal) {
        String filename = fileStorageService.getFilename(fileId, principal.getId());
        byte[] fileBytes = fileStorageService.downloadFile(fileId, principal.getId(), userPassword);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(fileBytes);
    }

    @DeleteMapping("/api/files/{fileId}")
    @Tag(name = "Files")
    @Operation(summary = "Delete a file and its chunks")
    public ResponseEntity<ApiResponse<Void>> deleteFile(
            @PathVariable UUID fileId,
            @AuthenticationPrincipal UserPrincipal principal) {
        fileStorageService.deleteFile(fileId, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("File deleted", null));
    }

    // ── Admin / Node management endpoints ─────────────────────────────────────

    @GetMapping("/api/admin/nodes")
    @Tag(name = "Admin")
    @Operation(summary = "List all registered storage nodes")
    public ResponseEntity<ApiResponse<List<StorageNodeResponse>>> listNodes() {
        return ResponseEntity.ok(ApiResponse.ok(storageNodeService.listNodes()));
    }

    @PostMapping("/api/admin/nodes")
    @Tag(name = "Admin")
    @Operation(summary = "Register a new storage node")
    public ResponseEntity<ApiResponse<StorageNodeResponse>> registerNode(
            @Valid @RequestBody RegisterNodeRequest request) {
        StorageNodeResponse node = storageNodeService.registerNode(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Node registered", node));
    }

    @GetMapping("/api/admin/files/{fileId}/chunks")
    @Tag(name = "Admin")
    @Operation(summary = "Get per-chunk replica distribution for a file")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getChunkMap(@PathVariable UUID fileId) {
        Map<String, Object> chunkMap = fileStorageService.getChunkMap(fileId);
        return ResponseEntity.ok(ApiResponse.ok(chunkMap));
    }

    @PostMapping("/api/admin/repair")
    @Tag(name = "Admin")
    @Operation(summary = "Trigger replication repair job immediately")
    public ResponseEntity<ApiResponse<String>> triggerRepair() {
        replicationRepairService.repairUnderReplicatedChunks();
        return ResponseEntity.ok(ApiResponse.ok("Repair job triggered", null));
    }
}
