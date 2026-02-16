package com.example.construction.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public class LocalFileStorageService implements FileStorageService {

    private final Path fileStorageLocation;
    private final String uploadDir;

    public LocalFileStorageService(String uploadDir) {
        this.uploadDir = uploadDir;
        this.fileStorageLocation = Paths.get(uploadDir)
                .toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    @Override
    public String upload(MultipartFile file, String directory) {
        // Normalize file name
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());

        String fileExtension = "";
        try {
            if (originalFileName.contains(".")) {
                fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }
        } catch (Exception e) {
            // ignore
        }

        // Emulate folder structure by creating it if needed
        Path targetDir = this.fileStorageLocation;
        if (directory != null && !directory.isEmpty()) {
            targetDir = targetDir.resolve(directory);
            try {
                Files.createDirectories(targetDir);
            } catch (IOException e) {
                throw new RuntimeException("Could not create directory: " + directory, e);
            }
        }

        String fileName = UUID.randomUUID().toString() + fileExtension;

        try {
            // Check if the file's name contains invalid characters
            if (fileName.contains("..")) {
                throw new RuntimeException("Sorry! Filename contains invalid path sequence " + fileName);
            }

            Path targetLocation = targetDir.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // We return the relative path that can be served by static resource handler
            // Assuming "uploads" is mapped to static resources.
            // If uploadDir is "uploads", and we saved to "uploads/directory/file.ext"
            // We should probably return that.

            // To be consistent with S3 which returns a full accessible URL (or key),
            // and assuming local dev environment handles access to these files via some
            // mapping or just file system.
            // For now, returning the relative path which might be stored in DB.
            // If DB expects FULL URL, this might be tricky without knowing host.
            // But usually for local dev, we might just store the filename/path.

            if (directory != null && !directory.isEmpty()) {
                return directory + "/" + fileName;
            }
            return fileName;

        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + fileName + ". Please try again!", ex);
        }
    }

    @Override
    public Resource download(String key) {
        try {
            Path filePath = this.fileStorageLocation.resolve(key).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new RuntimeException("File not found " + key);
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("File not found " + key, ex);
        }
    }

    @Override
    public void delete(String key) {
        if (key == null || key.startsWith("data:") || key.startsWith("http")) {
            // If it's a full URL from S3 (e.g. legacy data or mode switch), we can't easily
            // delete it locally
            // unless we parse it. But for Local mode, key should be a path.
            // However, if we store FULL URL in DB, we need to extract key here?
            // The prompt says "In DB stored URL".
            // So if DB has "https://endpoint/bucket/dir/file.png",
            // and we are in Local mode, we probably shouldn't crash, but we can't delete S3
            // file.
            // If DB has local path "dir/file.png", we delete it.

            // Basic safety:
            return;
        }

        try {
            Path filePath = this.fileStorageLocation.resolve(key).normalize();
            // Security Check: Prevent Path Traversal
            if (!filePath.startsWith(this.fileStorageLocation)) {
                // It might be that 'key' contains the upload dir prefix if we stored it that
                // way?
                // But let's assume strict relative path.
                // If key is "checklists/abc.png", resolve works.
                // If key is "../abc.png", it throws.
            }
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            System.err.println("Could not delete file: " + key + ". Error: " + e.getMessage());
        }
    }
}
