package com.example.construction.config;

import com.example.construction.service.FileStorageService;
import com.example.construction.service.LocalFileStorageService;
import com.example.construction.service.S3FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfiguration {

    @Bean
    @ConditionalOnProperty(name = "storage.type", havingValue = "local", matchIfMissing = true)
    public FileStorageService localFileStorageService(@Value("${file.upload-dir}") String uploadDir) {
        return new LocalFileStorageService(uploadDir);
    }

    @Bean
    @ConditionalOnProperty(name = "storage.type", havingValue = "s3")
    public FileStorageService s3FileStorageService(
            @Value("${s3.endpoint}") String endpoint,
            @Value("${s3.bucket}") String bucket,
            @Value("${s3.region}") String region,
            @Value("${s3.access-key}") String accessKey,
            @Value("${s3.secret-key}") String secretKey) {
        return new S3FileStorageService(endpoint, bucket, region, accessKey, secretKey);
    }
}
