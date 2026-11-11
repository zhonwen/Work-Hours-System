package com.gsms.gsms.infra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 本地文件上传配置（头像等）
 */
@ConfigurationProperties(prefix = "gsms.upload")
public class UploadProperties {

    /** 本地存储根目录 */
    private String baseDir = "./data/uploads";

    /** 头像最大字节数，默认 1MB */
    private long avatarMaxBytes = 1048576L;

    /** 对外访问前缀（不含域名） */
    private String publicPathPrefix = "/uploads";

    public String getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(String baseDir) {
        this.baseDir = baseDir;
    }

    public long getAvatarMaxBytes() {
        return avatarMaxBytes;
    }

    public void setAvatarMaxBytes(long avatarMaxBytes) {
        this.avatarMaxBytes = avatarMaxBytes;
    }

    public String getPublicPathPrefix() {
        return publicPathPrefix;
    }

    public void setPublicPathPrefix(String publicPathPrefix) {
        this.publicPathPrefix = publicPathPrefix;
    }
}
