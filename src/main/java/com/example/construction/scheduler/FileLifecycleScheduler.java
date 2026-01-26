package com.example.construction.scheduler;

import com.example.construction.model.StoredFile;
import com.example.construction.reposirtories.StoredFileRepository;
import com.example.construction.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class FileLifecycleScheduler {

    private final StoredFileRepository storedFileRepository;
    private final S3Service s3Service;

    @Scheduled(cron = "${file.lifecycle.cron:0 0 2 * * ?}") // Default 2 AM daily
    public void deleteExpiredFiles() {
        log.info("Starting file lifecycle cleanup check...");

        List<StoredFile> expiredFiles = storedFileRepository.findByExpiredAtBefore(LocalDateTime.now());

        if (expiredFiles.isEmpty()) {
            log.info("No expired files found.");
            return;
        }

        log.info("Found {} expired files. Starting deletion...", expiredFiles.size());

        for (StoredFile file : expiredFiles) {
            try {
                s3Service.deleteFile(file);
                log.info("Deleted expired file: {}", file.getS3Key());
            } catch (Exception e) {
                log.error("Failed to delete expired file: " + file.getS3Key(), e);
            }
        }

        log.info("File lifecycle cleanup completed.");
    }
}
