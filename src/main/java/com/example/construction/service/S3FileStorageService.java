package com.example.construction.service;

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

            // Construct public URL
            // Format: https://<endpoint>/<bucket>/<key>
            // Note: Endpoint might contain https:// already.
            String baseUrl = endpoint;
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }

            return baseUrl + "/" + bucketName + "/" + key;

        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file to S3", e);
        }
    }

    @Override
    public Resource download(String key) {
        // Since the key stored in DB is the full URL (based on prompt "В БД хранится
        // URL"),
        // we might need to extract the key if we were using GetObject.
        // BUT, the prompt says "Resource download(String key)".
        // And usually download means returning the bytes or a resource.
        // If the file is public, we can just return a UrlResource to the public URL.
        // Or if 'key' passed here is just the key?
        // Let's assume 'key' passed to download is the key relative to bucket, or the
        // full URL?
        // The interface signature is `download(String key)`.
        // Usages in code: `Resource resource = fileStorageService.download(key)`. (None
        // yet, strictly speaking, existing code doesn't use download much, mostly
        // static serving).

        // If existing usages rely on `Path`, we might have issues.
        // But the prompt says "Bизнес-логика НЕ работает с File / Path".

        // If we store full URL in DB, then when we call download, we probably pass the
        // full URL?
        // Or do we pass the key?
        // If we pass the full URL, we can just return UrlResource.

        try {
            if (key.startsWith("http")) {
                return new UrlResource(URI.create(key));
            }
            // Fallback if it's just a key, construct URL
            String baseUrl = endpoint;
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            return new UrlResource(URI.create(baseUrl + "/" + bucketName + "/" + key));
        } catch (MalformedURLException e) {
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
            // format: https://endpoint/bucket/KEY
            // find bucket name and take everything after
            // simple parse:
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
