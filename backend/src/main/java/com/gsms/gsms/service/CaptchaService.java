package com.gsms.gsms.service;

import com.gsms.gsms.dto.captcha.CaptchaResp;

/**
 * 登录图形验证码
 */
public interface CaptchaService {

    /**
     * 生成验证码会话（含图片与公钥）
     */
    CaptchaResp createCaptcha();

    /**
     * 校验并一次性消费验证码；失败抛业务异常
     */
    void verifyAndConsume(String cid, String captchaCode);

    /**
     * 仅供测试：创建指定答案的验证码会话，返回 cid
     */
    String createCaptchaForTest(String answer);
}
