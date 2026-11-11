package com.gsms.gsms.model.enums.errorcode;

import com.gsms.gsms.infra.exception.ErrorCode;

/**
 * 用户模块错误码枚举 2xxx
 * HTTP状态码建议：
 * - 2001-2006: 业务验证错误，建议HTTP 400
 * - 2901-2903: 操作失败，建议HTTP 500
 */
public enum UserErrorCode implements ErrorCode {
    
    // ========== 业务验证错误（建议HTTP 400） ==========
    USERNAME_EXISTS(2001, "用户名已存在"),
    USER_NOT_FOUND(2002, "用户不存在"),            // 也可 HTTP 404
    PASSWORD_ERROR(2003, "密码错误"),               // 也可 HTTP 401
    EMAIL_FORMAT_ERROR(2004, "邮箱格式错误"),
    PHONE_FORMAT_ERROR(2005, "手机号格式错误"),
    USER_DISABLED(2006, "用户已禁用"),             // 也可 HTTP 403
    DEFAULT_ROLE_NOT_FOUND(2007, "默认角色不存在"), // EMPLOYEE角色未配置
    CAPTCHA_INVALID(2008, "验证码错误或已过期"),
    LOGIN_DECRYPT_FAILED(2009, "密码解密失败"),
    LOGIN_CID_MISMATCH(2010, "登录凭证校验失败"),
    PASSWORD_LENGTH_INVALID(2011, "密码长度必须在6-20位之间"),
    AVATAR_FILE_EMPTY(2012, "请选择头像文件"),
    AVATAR_TOO_LARGE(2013, "头像大小不能超过1MB"),
    AVATAR_TYPE_INVALID(2014, "头像仅支持 JPG、PNG、WEBP 格式"),
    AVATAR_PRESET_INVALID(2015, "无效的预设头像"),
    
    // ========== 操作失败（建议HTTP 500） ==========
    USER_CREATE_FAILED(2901, "用户创建失败"),
    USER_UPDATE_FAILED(2902, "用户更新失败"),
    USER_DELETE_FAILED(2903, "用户删除失败"),
    AVATAR_UPLOAD_FAILED(2904, "头像上传失败");
    
    private final int code;
    private final String message;
    
    UserErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
    
    @Override
    public int getCode() {
        return code;
    }
    
    @Override
    public String getMessage() {
        return message;
    }
}
