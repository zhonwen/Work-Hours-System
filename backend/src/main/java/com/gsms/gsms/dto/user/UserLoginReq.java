package com.gsms.gsms.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;

/**
 * 用户登录请求（密码为 RSA 密文：明文密码|cid）
 */
@Schema(description = "用户登录请求")
public class UserLoginReq {
    
    @NotBlank(message = "用户名不能为空")
    @Schema(description = "用户名", example = "zhangsan")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    @Schema(description = "RSA 加密后的密文（明文格式：密码|cid）", example = "base64...")
    private String password;

    @NotBlank(message = "验证码会话不能为空")
    @Schema(description = "验证码会话 ID，需与密文内 cid 一致")
    private String cid;

    @NotBlank(message = "验证码不能为空")
    @Schema(description = "用户输入的验证码")
    private String captchaCode;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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
