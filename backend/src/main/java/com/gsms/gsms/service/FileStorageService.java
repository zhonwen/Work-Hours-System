package com.gsms.gsms.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储抽象（一期本地磁盘；后续可换对象存储）
 */
public interface FileStorageService {

    /**
     * 保存用户头像，返回对外访问路径（如 /uploads/avatars/1/xxx.webp）
     */
    String storeAvatar(Long userId, MultipartFile file);
}
