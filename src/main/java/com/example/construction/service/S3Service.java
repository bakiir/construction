package com.example.construction.service;

import com.example.construction.model.StoredFile;
import com.example.construction.reposirtories.StoredFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final StoredFileRepository storedFileRepository;

    @Value("${s3.bucket}")
    private String bucket;

    @Transactional
    public StoredFile uploadFile(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String key = UUID.randomUUID().toString() + extension;

        try {
            PutObjectRequest putOb = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            log.info("Starting upload to S3: bucket={}, key={}, size={}", bucket, key, file.getSize());
            s3Client.putObject(putOb, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            log.info("Upload success: bucket={}, key={}", bucket, key);

            StoredFile storedFile = StoredFile.builder()
                    .s3Key(key)
                    .originalFilename(originalFilename)
                    .contentType(file.getContentType())
                    .size(file.getSize())
                    .bucket(bucket)
                    .build();

            return storedFileRepository.save(storedFile);

        } catch (IOException e) {
            log.error("Failed to upload file to S3", e);
            throw new RuntimeException("Failed to upload file to S3", e);
        } catch (software.amazon.awssdk.core.exception.SdkException e) {
            log.error("AWS SDK error during upload: bucket={}, key={}", bucket, key, e);
            throw new RuntimeException("AWS SDK error during upload", e);
        }
    }

    public String generatePresignedUrl(String key) {
        GetObjectPresignRequest getObjectPresignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(60)) // Link valid for 60 minutes
                .getObjectRequest(b -> b.bucket(bucket).key(key))
                .build();

        PresignedGetObjectRequest presignedGetObjectRequest = s3Presigner.presignGetObject(getObjectPresignRequest);
        return presignedGetObjectRequest.url().toString();
    }

    public String generatePresignedUrl(StoredFile file) {
        return generatePresignedUrl(file.getS3Key());
    }

    @Transactional
    public void deleteFile(StoredFile file) {
        try {
            s3Client.deleteObject(b -> b.bucket(file.getBucket()).key(file.getS3Key()));
            storedFileRepository.delete(file);
        } catch (Exception e) {
            log.error("Failed to delete file from S3: " + file.getS3Key(), e);
            // We might still want to delete the DB record or mark it for retry
        }
    }
}
