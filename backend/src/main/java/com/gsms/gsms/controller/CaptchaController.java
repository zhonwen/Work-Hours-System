package com.gsms.gsms.controller;

import com.gsms.gsms.dto.captcha.CaptchaResp;
import com.gsms.gsms.infra.common.Result;
import com.gsms.gsms.service.CaptchaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录验证码
 */
@RestController
@RequestMapping("/api/captcha")
@Tag(name = "验证码接口", description = "登录图形验证码与 RSA 公钥下发")
public class CaptchaController {

    private final CaptchaService captchaService;

    public CaptchaController(CaptchaService captchaService) {
        this.captchaService = captchaService;
    }

    @GetMapping
    @Operation(summary = "获取登录验证码（含 RSA 公钥）")
    public Result<CaptchaResp> getCaptcha() {
        return Result.success(captchaService.createCaptcha());
    }
}
