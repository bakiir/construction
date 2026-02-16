package com.example.construction.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String upload(MultipartFile file, String directory);

    Resource download(String key);

    void delete(String key);
}
