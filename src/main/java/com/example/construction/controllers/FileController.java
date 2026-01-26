package com.example.construction.controllers;

import com.example.construction.service.S3Service;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {

    private final S3Service s3Service;

    @GetMapping("/{fileName}")
    public ResponseEntity<Resource> getFile(@PathVariable String fileName) {
        try {
            // For simplicity, we assume the filename is the key or we can look it up if
            // needed.
            // However, our new design stores the key in the database.
            // If the frontend requests by filename, we might need to lookup by original
            // filename or S3 key.
            // But for now, let's assume the frontend will be updated to use the full URL
            // from the DTO.

            // If we want to support the old endpoint pattern /files/{key}, we can redirect.
            String url = s3Service.generatePresignedUrl(fileName); // Expecting fileName to be the key here

            return ResponseEntity.status(307) // Temporary Redirect
                    .location(java.net.URI.create(url))
                    .build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
