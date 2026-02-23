package com.collabstack.storage.service;

import com.collabstack.storage.dto.response.UploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface FileUploadService {
    UploadResponse upload(UUID userId, MultipartFile file, String userPassword);
}
