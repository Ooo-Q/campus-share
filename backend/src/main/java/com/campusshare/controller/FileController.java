package com.campusshare.controller;

import com.campusshare.config.FileStorageProperties;
import com.campusshare.service.FileStorageService;
import com.campusshare.util.SecurityUtil;
import com.campusshare.vo.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;
    private final FileStorageProperties properties;
    private final com.campusshare.service.PunishmentService punishmentService;

    @PostMapping("/upload")
    public ApiResponse<String> upload(@RequestParam("file") MultipartFile file) {
        Long userId = SecurityUtil.requireLogin();
        punishmentService.checkUploadAllowed(userId);
        String url = fileStorageService.store(file);
        return ApiResponse.success("上传成功", url);
    }

    @PostMapping("/upload/avatar")
    public ApiResponse<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        SecurityUtil.requireLogin();

        if (file == null || file.isEmpty()) {
            throw new com.campusshare.exception.BusinessException("文件不能为空");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new com.campusshare.exception.BusinessException("只能上传图片文件");
        }

        if (file.getSize() > 5 * 1024 * 1024) {
            throw new com.campusshare.exception.BusinessException("图片大小不能超过5MB");
        }

        try {
            String url = fileStorageService.store(file);
            return ApiResponse.success("头像上传成功", url);
        } catch (com.campusshare.exception.BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new com.campusshare.exception.BusinessException("头像上传失败: " + e.getMessage());
        }
    }

    @GetMapping("/public/{fileName:.+}")
    public ResponseEntity<FileSystemResource> download(@PathVariable String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return ResponseEntity.notFound().build();
        }

        String uploadDir = properties.getUploadDir();
        Path filePath;
        if (uploadDir.startsWith("./") || !Paths.get(uploadDir).isAbsolute()) {
            String projectRoot = System.getProperty("user.dir");
            if (projectRoot.endsWith("backend")) {
                filePath = Paths.get(projectRoot, uploadDir.replace("./", ""), fileName);
            } else {
                filePath = Paths.get(projectRoot, "backend", uploadDir.replace("./", ""), fileName);
            }
        } else {
            filePath = Paths.get(uploadDir, fileName);
        }

        File file = filePath.toFile();

        log.debug("尝试下载文件: uploadDir={}, fileName={}, fullPath={}, exists={}",
                properties.getUploadDir(), fileName, filePath.toAbsolutePath(), file.exists());

        if (!file.exists()) {
            String projectRoot = System.getProperty("user.dir");
            Path[] altPaths = {
                Paths.get(projectRoot, "backend", "uploads", fileName),
                Paths.get(projectRoot, "uploads", fileName),
                Paths.get("backend", "uploads", fileName),
                Paths.get("uploads", fileName),
            };

            boolean found = false;
            for (Path altPath : altPaths) {
                File altFile = altPath.toFile();
                log.debug("尝试备用路径: {} -> 存在: {}", altPath.toAbsolutePath(), altFile.exists());
                if (altFile.exists()) {
                    file = altFile;
                    filePath = altPath;
                    found = true;
                    log.info("找到文件，使用路径: {}", altPath.toAbsolutePath());
                    break;
                }
            }

            if (!found) {
                log.warn("所有路径都未找到文件，返回404: fileName={}", fileName);
                return ResponseEntity.notFound().build();
            }
        }

        FileSystemResource resource = new FileSystemResource(file);

        String contentType = "application/octet-stream";
        String fileNameLower = fileName.toLowerCase();
        if (fileNameLower.endsWith(".pdf")) {
            contentType = "application/pdf";
        } else if (fileNameLower.endsWith(".doc") || fileNameLower.endsWith(".docx")) {
            contentType = "application/msword";
        } else if (fileNameLower.endsWith(".xls") || fileNameLower.endsWith(".xlsx")) {
            contentType = "application/vnd.ms-excel";
        } else if (fileNameLower.endsWith(".txt")) {
            contentType = "text/plain";
        } else if (fileNameLower.endsWith(".jpg") || fileNameLower.endsWith(".jpeg")) {
            contentType = "image/jpeg";
        } else if (fileNameLower.endsWith(".png")) {
            contentType = "image/png";
        } else if (fileNameLower.endsWith(".gif")) {
            contentType = "image/gif";
        } else if (fileNameLower.endsWith(".zip")) {
            contentType = "application/zip";
        } else if (fileNameLower.endsWith(".rar")) {
            contentType = "application/x-rar-compressed";
        }

        String originalFileName = file.getName();

        // Images must be served without attachment disposition so <img> can display them
        var responseBuilder = ResponseEntity.ok()
                .contentLength(file.length())
                .contentType(MediaType.parseMediaType(contentType));

        if (contentType.startsWith("image/")) {
            return responseBuilder
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                    .body(resource);
        }

        return responseBuilder
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + encodeFileName(originalFileName) + "\"")
                .body(resource);
    }

    private String encodeFileName(String fileName) {
        try {
            return java.net.URLEncoder.encode(fileName, "UTF-8").replace("+", "%20");
        } catch (Exception e) {
            return fileName;
        }
    }
}
