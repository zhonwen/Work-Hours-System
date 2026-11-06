package com.gsms.gsms.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotBlank;

/**
 * 修改密码请求（oldPassword / newPassword 均为 RSA 密文：明文密码|cid）
 */
@Schema(description = "修改密码请求")
public class PasswordChangeReq {

    @Schema(description = "旧密码 RSA 密文（明文格式：密码|cid）", required = true)
    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    @Schema(description = "新密码 RSA 密文（明文格式：密码|cid）", required = true)
    @NotBlank(message = "新密码不能为空")
    private String newPassword;

    @NotBlank(message = "验证码会话不能为空")
    @Schema(description = "验证码会话 ID")
    private String cid;

    @NotBlank(message = "验证码不能为空")
    @Schema(description = "用户输入的验证码")
    private String captchaCode;

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
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
