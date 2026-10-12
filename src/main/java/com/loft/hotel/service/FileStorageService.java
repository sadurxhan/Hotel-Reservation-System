package com.loft.hotel.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    // only real image types are accepted
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final Path root;   // the uploads folder on disk

    public FileStorageService(@Value("${app.upload-dir:uploads}") String uploadDir) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);          // create the folder if it doesn't exist
        } catch (IOException e) {
            throw new IllegalStateException("Could not create upload folder", e);
        }
    }

    /**
     * Saves the file under uploads/<subFolder>/ and returns the URL path
     * that gets stored in the database (e.g. /uploads/menu/3f2a....jpg)
     */
    public String store(MultipartFile file, String subFolder) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No file selected");
        }
        String type = file.getContentType();
        if (type == null || !ALLOWED_TYPES.contains(type)) {
            throw new IllegalArgumentException("Only JPG, PNG or WEBP images are allowed");
        }

        // We NEVER use the guest's/admin's original file name:
        // a random UUID name stops two files overwriting each other
        // and blocks "../../" path tricks in file names.
        String extension = switch (type) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
        String fileName = UUID.randomUUID() + extension;

        try {
            Path folder = root.resolve(subFolder);
            Files.createDirectories(folder);
            Files.copy(file.getInputStream(), folder.resolve(fileName),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Could not save the file", e);
        }
        return "/uploads/" + subFolder + "/" + fileName;
    }
}