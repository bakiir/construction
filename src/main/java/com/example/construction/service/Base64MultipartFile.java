package com.example.construction.service;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.FileCopyUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

public class Base64MultipartFile implements MultipartFile {

    private final byte[] content;
    private final String header;
    private final String contentType;
    private final String originalFilename;

    public Base64MultipartFile(String base64, String originalFilename) {
        if (base64 == null)
            throw new IllegalArgumentException("Base64 string cannot be null");

        String[] parts = base64.split(",");
        if (parts.length > 1) {
            this.header = parts[0];
            this.content = Base64.getDecoder().decode(parts[1]);
        } else {
            // Assume raw base64 if no header, though usually it comes with data:image/...
            this.header = "";
            this.content = Base64.getDecoder().decode(parts[0]);
        }

        this.originalFilename = originalFilename;
        this.contentType = extractContentType(this.header);
    }

    private String extractContentType(String header) {
        if (header.contains("image/png"))
            return "image/png";
        if (header.contains("image/jpeg"))
            return "image/jpeg";
        if (header.contains("image/webp"))
            return "image/webp";
        if (header.contains("image/gif"))
            return "image/gif";
        return "application/octet-stream";
    }

    @Override
    public String getName() {
        return "file";
    }

    @Override
    public String getOriginalFilename() {
        return originalFilename;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean isEmpty() {
        return content == null || content.length == 0;
    }

    @Override
    public long getSize() {
        return content.length;
    }

    @Override
    public byte[] getBytes() throws IOException {
        return content;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(content);
    }

    @Override
    public void transferTo(File dest) throws IOException, IllegalStateException {
        FileCopyUtils.copy(content, dest);
    }
}
