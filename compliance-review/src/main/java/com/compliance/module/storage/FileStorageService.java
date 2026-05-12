package com.compliance.module.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {

    @Value("${compliance.upload.path}")
    private String uploadPath;

    public String saveFile(MultipartFile file, Long tenantId) {
        String originalFilename = file.getOriginalFilename();
        String extension = getExtension(originalFilename);
        String storedFileName = UUID.randomUUID().toString() + "." + extension;

        Path tenantDir = Paths.get(uploadPath, String.valueOf(tenantId));
        try {
            Files.createDirectories(tenantDir);
            Path filePath = tenantDir.resolve(storedFileName);
            Files.write(filePath, file.getBytes());
            log.info("File saved: {}", filePath);
            return tenantId + "/" + storedFileName;
        } catch (IOException e) {
            log.error("Failed to save file: {}", e.getMessage());
            throw new RuntimeException("文件保存失败: " + e.getMessage(), e);
        }
    }

    private String getExtension(String fileName) {
        if (fileName == null) {
            return "bin";
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "bin";
        }
        return fileName.substring(dotIndex + 1);
    }
}
