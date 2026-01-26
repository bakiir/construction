package com.example.construction.reposirtories;

import com.example.construction.model.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {
    Optional<StoredFile> findByS3Key(String s3Key);

    List<StoredFile> findByExpiredAtBefore(LocalDateTime now);
}
