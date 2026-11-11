package com.gsms.gsms.service.impl;

import com.gsms.gsms.infra.config.UploadProperties;
import com.gsms.gsms.infra.exception.BusinessException;
import com.gsms.gsms.model.enums.errorcode.UserErrorCode;
import com.gsms.gsms.service.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 本地磁盘头像存储
 */
@Service
public class LocalFileStorageService implements FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(LocalFileStorageService.class);

    private static final Set<String> ALLOWED_EXT = new HashSet<>(Arrays.asList("jpg", "jpeg", "png", "webp"));
    private static final Set<String> ALLOWED_CONTENT = new HashSet<>(Arrays.asList(
            "image/jpeg", "image/png", "image/webp", "image/jpg"
    ));

    private final UploadProperties uploadProperties;

    public LocalFileStorageService(UploadProperties uploadProperties) {
        this.uploadProperties = uploadProperties;
    }

    @Override
    public String storeAvatar(Long userId, MultipartFile file) {
        if (userId == null) {
            throw new BusinessException(UserErrorCode.USER_NOT_FOUND);
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException(UserErrorCode.AVATAR_FILE_EMPTY);
        }
        if (file.getSize() > uploadProperties.getAvatarMaxBytes()) {
            throw new BusinessException(UserErrorCode.AVATAR_TOO_LARGE);
        }

        String original = file.getOriginalFilename();
        String ext = resolveExtension(original, file.getContentType());
        if (ext == null || !ALLOWED_EXT.contains(ext)) {
            throw new BusinessException(UserErrorCode.AVATAR_TYPE_INVALID);
        }
        String contentType = file.getContentType();
        if (StringUtils.hasText(contentType) && !ALLOWED_CONTENT.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BusinessException(UserErrorCode.AVATAR_TYPE_INVALID);
        }

        String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path dir = Paths.get(uploadProperties.getBaseDir(), "avatars", String.valueOf(userId))
                .toAbsolutePath()
                .normalize();
        try {
            Files.createDirectories(dir);
            Path target = dir.resolve(filename).normalize();
            if (!target.startsWith(dir)) {
                throw new BusinessException(UserErrorCode.AVATAR_UPLOAD_FAILED);
            }
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            logger.error("保存头像失败: userId={}", userId, e);
            throw new BusinessException(UserErrorCode.AVATAR_UPLOAD_FAILED);
        }

        String prefix = uploadProperties.getPublicPathPrefix();
        if (prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        String publicPath = prefix + "/avatars/" + userId + "/" + filename;
        logger.info("头像已保存: userId={}, path={}", userId, publicPath);
        return publicPath;
    }

    private String resolveExtension(String originalFilename, String contentType) {
        if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
            String ext = originalFilename.substring(originalFilename.lastIndexOf('.') + 1)
                    .toLowerCase(Locale.ROOT)
                    .trim();
            if (ALLOWED_EXT.contains(ext)) {
                return "jpeg".equals(ext) ? "jpg" : ext;
            }
        }
        if (!StringUtils.hasText(contentType)) {
            return null;
        }
        String ct = contentType.toLowerCase(Locale.ROOT);
        if (ct.contains("jpeg") || ct.contains("jpg")) {
            return "jpg";
        }
        if (ct.contains("png")) {
            return "png";
        }
        if (ct.contains("webp")) {
            return "webp";
        }
        return null;
    }
}
