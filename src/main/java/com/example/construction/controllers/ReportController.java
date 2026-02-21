package com.example.construction.controllers;

import com.example.construction.dto.ReportCreateDto;
import com.example.construction.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final ObjectMapper objectMapper;

    @PostMapping("/tasks/{taskId}/report")
    // @PreAuthorize("hasRole('WORKER')")
    public ResponseEntity<Void> createReport(
            @PathVariable Long taskId,
            @RequestPart("report") String reportJson,
            @RequestPart("files") List<MultipartFile> files,
            Authentication authentication) {

        System.out.println("DEBUG: createReport called for task " + taskId);
        if (authentication == null) {
            System.out.println("DEBUG: Authentication is null");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        System.out.println("DEBUG: User: " + authentication.getName());
        System.out.println("DEBUG: Authorities: " + authentication.getAuthorities());

        boolean isWorker = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_WORKER"));

        if (!isWorker) {
            System.out.println("DEBUG: Access Denied. User is not ROLE_WORKER");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            ReportCreateDto reportDto = objectMapper.readValue(reportJson, ReportCreateDto.class);

            // Basic validation from TZ
            if (files.isEmpty() || files.size() > 3) {
                return ResponseEntity.badRequest().build();
            }

            String authorPhone = authentication.getName();
            reportService.createReport(taskId, reportDto, files, authorPhone);

            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (Exception e) {
            // Log the exception
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
