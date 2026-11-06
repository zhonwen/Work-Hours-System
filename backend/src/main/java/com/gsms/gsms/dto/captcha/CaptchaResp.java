package com.gsms.gsms.dto.captcha;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 登录验证码响应
 */
@Schema(description = "登录验证码响应")
public class CaptchaResp {

    @Schema(description = "验证码会话 ID")
    private String cid;

    @Schema(description = "RSA 公钥 PEM，用于加密 密码|cid")
    private String publicKey;

    @Schema(description = "验证码图片 Base64（含 data URI 前缀）")
    private String imageBase64;

    public CaptchaResp() {
    }

    public CaptchaResp(String cid, String publicKey, String imageBase64) {
        this.cid = cid;
        this.publicKey = publicKey;
        this.imageBase64 = imageBase64;
    }

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }
}
