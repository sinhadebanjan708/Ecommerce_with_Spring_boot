package com.elvero.shop.service;

import com.elvero.shop.exception.ApiException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

    private static final Set<String> ALLOWED = Set.of("image/jpeg", "image/png", "image/webp");

    private final Path uploadRoot;

    public FileStorageService(@Value("${app.upload-dir}") String uploadDir) {
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    public String store(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED.contains(contentType)) {
            throw ApiException.badRequest("Only JPG, PNG and WEBP images are allowed");
        }
        try {
            Path dir = uploadRoot.resolve(folder);
            Files.createDirectories(dir);
            String ext = extension(file.getOriginalFilename());
            String filename = folder + "-" + UUID.randomUUID() + ext;
            Path target = dir.resolve(filename);
            file.transferTo(target);
            return "uploads/" + folder + "/" + filename;
        } catch (IOException e) {
            throw ApiException.badRequest("Failed to store file: " + e.getMessage());
        }
    }

    private static String extension(String name) {
        if (name == null) return "";
        int i = name.lastIndexOf('.');
        return i >= 0 ? name.substring(i).toLowerCase() : "";
    }
}
