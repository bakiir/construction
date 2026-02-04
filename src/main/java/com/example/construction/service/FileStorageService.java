package com.example.construction.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path fileStorageLocation;

    public FileStorageService(@Value("${file.upload-dir}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir)
                .toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    public String storeFile(MultipartFile file) {
        // Normalize file name
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null) {
            throw new RuntimeException("File name is null");
        }
        String fileExtension = "";
        try {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        } catch (Exception e) {
            // ignore
        }
        String fileName = UUID.randomUUID().toString() + fileExtension;

        try {
            // Check if the file's name contains invalid characters
            if (fileName.contains("..")) {
                throw new RuntimeException("Sorry! Filename contains invalid path sequence " + fileName);
            }

            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return fileName;
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + fileName + ". Please try again!", ex);
        }
    }

    public String storeBase64File(String base64Data) {
        if (base64Data == null || !base64Data.startsWith("data:image")) {
            return base64Data;
        }

        try {
            String[] parts = base64Data.split(",");
            String metadata = parts[0];
            String base64Content = parts[1];

            String extension = ".png";
            if (metadata.contains("image/jpeg")) {
                extension = ".jpg";
            } else if (metadata.contains("image/webp")) {
                extension = ".webp";
            }

            String fileName = UUID.randomUUID().toString() + extension;
            byte[] decodedBytes = Base64.getDecoder().decode(base64Content);

            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.write(targetLocation, decodedBytes);

            return fileName;
        } catch (Exception e) {
            throw new RuntimeException("Failed to store base64 image", e);
        }
    }

    public void deleteFile(String fileName) {
        if (fileName == null || fileName.startsWith("data:") || fileName.startsWith("http")) {
            return; // Don't try to delete base64 strings or remote URLs
        }

        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            // Security Check: Prevent Path Traversal
            if (!filePath.startsWith(this.fileStorageLocation)) {
                throw new SecurityException("Cannot delete file outside of upload directory: " + fileName);
            }
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // Log but don't fail the whole operation if file delete fails
            System.err.println("Could not delete file: " + fileName + ". Error: " + e.getMessage());
        }
    }
}
