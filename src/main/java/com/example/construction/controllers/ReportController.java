package com.example.construction.controllers;

import com.example.construction.dto.ReportCreateDto;
import com.example.construction.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final ObjectMapper objectMapper;

    @PostMapping("/tasks/{taskId}/report")
    @PreAuthorize("hasRole('WORKER')")
    public ResponseEntity<Void> createReport(
            @PathVariable Long taskId,
            @RequestPart("report") String reportJson,
            @RequestPart("files") List<MultipartFile> files,
            Authentication authentication) {

        try {
            ReportCreateDto reportDto = objectMapper.readValue(reportJson, ReportCreateDto.class);

            // Basic validation from TZ
            if (files.isEmpty() || files.size() > 3) {
                return ResponseEntity.badRequest().build();
            }

            String authorEmail = authentication.getName();
            reportService.createReport(taskId, reportDto, files, authorEmail);

            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (RuntimeException e) {
            // Log the exception
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
