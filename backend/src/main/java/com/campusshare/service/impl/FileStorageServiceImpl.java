package com.campusshare.service.impl;

import com.campusshare.config.FileStorageProperties;
import com.campusshare.exception.BusinessException;
import com.campusshare.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    private final FileStorageProperties fileStorageProperties;

    @Override
    public String store(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }
        try {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                originalFilename = "file";
            }
            originalFilename = StringUtils.cleanPath(originalFilename);

            String extension = "";
            int dotIndex = originalFilename.lastIndexOf('.');
            if (dotIndex >= 0) {
                extension = originalFilename.substring(dotIndex);
            }

            String newFilename = UUID.randomUUID() + extension;

            Path uploadDir = Paths.get(fileStorageProperties.getUploadDir());
            Files.createDirectories(uploadDir);

            Path targetPath = uploadDir.resolve(newFilename);

            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            return "/files/public/" + newFilename;
        } catch (IOException e) {
            throw new BusinessException("文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public void delete(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }

        try {
            String fileName = fileUrl;
            if (fileUrl.contains("/")) {
                fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            }

            if (fileName.isEmpty()) {
                log.warn("无法从文件URL中提取文件名: {}", fileUrl);
                return;
            }

            Path uploadDir = Paths.get(fileStorageProperties.getUploadDir());
            Path filePath = uploadDir.resolve(fileName);

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("成功删除文件: {}", filePath.toAbsolutePath());
            } else {
                log.warn("文件不存在，跳过删除: {}", filePath.toAbsolutePath());
            }
        } catch (IOException e) {
            log.warn("删除文件失败: {}, 错误: {}", fileUrl, e.getMessage());
        }
    }
}
