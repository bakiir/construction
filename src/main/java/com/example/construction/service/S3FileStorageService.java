package com.example.construction.service;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.UUID;

public class S3FileStorageService implements FileStorageService {

    private final S3Client s3Client;
    private final String bucketName;
    private final String endpoint;

    public S3FileStorageService(String endpoint, String bucketName, String region, String accessKey, String secretKey) {
        this.bucketName = bucketName;
        this.endpoint = endpoint;

        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }

    @Override
    public String upload(MultipartFile file, String directory) {
        try {
            String originalFileName = file.getOriginalFilename();
            String extension = "";
            if (originalFileName != null && originalFileName.contains(".")) {
                extension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }

            String fileName = UUID.randomUUID().toString() + extension;
            String key = (directory != null && !directory.isEmpty()) ? directory + "/" + fileName : fileName;

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            // Return just the key (e.g. "tasks/abc.jpg") so the frontend constructs
            // the backend proxy URL: http://localhost:8080/files/tasks/abc.jpg
            return key;

        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file to S3", e);
        }
    }

    @Override
    public Resource download(String key) {
        try {
            // Strip any accidental full URL prefix if present in legacy data
            String objectKey = key;
            if (key.startsWith("http")) {
                String marker = "/" + bucketName + "/";
                int index = key.indexOf(marker);
                if (index != -1) {
                    objectKey = key.substring(index + marker.length());
                }
            }

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();

            return new InputStreamResource(s3Client.getObject(getObjectRequest));
        } catch (Exception e) {
            throw new RuntimeException("Error resolving URL for key: " + key, e);
        }
    }

    @Override
    public void delete(String key) {
        if (key == null || key.isEmpty())
            return;

        // Extract key from URL if it is a URL
        String objectKey = key;
        if (key.startsWith("http")) {
            String marker = "/" + bucketName + "/";
            int index = key.indexOf(marker);
            if (index != -1) {
                objectKey = key.substring(index + marker.length());
            }
        }

        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
        } catch (Exception e) {
            System.err.println("Failed to delete file from S3: " + key);
        }
    }
}
