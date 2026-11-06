package com.gsms.gsms.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 重置密码请求（管理员；newPassword 为 RSA 密文：明文密码|cid）
 */
@Schema(description = "重置密码请求")
public class PasswordResetReq {

    @NotNull(message = "用户ID不能为空")
    @Schema(description = "用户ID", required = true, example = "1")
    private Long userId;

    @NotBlank(message = "新密码不能为空")
    @Schema(description = "新密码 RSA 密文（明文格式：密码|cid）", required = true)
    private String newPassword;

    @NotBlank(message = "验证码会话不能为空")
    @Schema(description = "验证码会话 ID")
    private String cid;

    @NotBlank(message = "验证码不能为空")
    @Schema(description = "用户输入的验证码")
    private String captchaCode;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public String getCaptchaCode() {
        return captchaCode;
    }

    public void setCaptchaCode(String captchaCode) {
        this.captchaCode = captchaCode;
    }
}
