package com.example.construction.controllers;

import com.example.construction.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    @GetMapping("/{*path}")
    public ResponseEntity<Resource> getFile(@PathVariable String path) {
        try {
            // Remove leading slash if present
            if (path.startsWith("/")) {
                path = path.substring(1);
            }

            Resource resource = fileStorageService.download(path);

            String contentType = "application/octet-stream";
            try {
                if (resource.exists() && resource.isFile()) {
                    contentType = Files.probeContentType(resource.getFile().toPath());
                } else {
                    // Start basic detection based on extension
                    if (path.endsWith(".jpg") || path.endsWith(".jpeg"))
                        contentType = "image/jpeg";
                    else if (path.endsWith(".png"))
                        contentType = "image/png";
                    else if (path.endsWith(".pdf"))
                        contentType = "application/pdf";
                }
            } catch (Exception e) {
                // ignore
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
