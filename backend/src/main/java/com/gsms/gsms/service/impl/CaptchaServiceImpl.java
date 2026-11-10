package com.gsms.gsms.service.impl;

import com.gsms.gsms.dto.captcha.CaptchaResp;
import com.gsms.gsms.infra.exception.BusinessException;
import com.gsms.gsms.infra.utils.RsaKeyHolder;
import com.gsms.gsms.model.enums.errorcode.UserErrorCode;
import com.gsms.gsms.service.CaptchaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 内存验证码：TTL 5 分钟，一次性消费
 */
@Service
public class CaptchaServiceImpl implements CaptchaService {

    private static final Logger logger = LoggerFactory.getLogger(CaptchaServiceImpl.class);

    private static final long TTL_MS = 5 * 60 * 1000L;
    private static final int CODE_LEN = 4;
    /** 排除易混淆字符 */
    private static final char[] CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    private final RsaKeyHolder rsaKeyHolder;
    private final ConcurrentHashMap<String, CaptchaEntry> store = new ConcurrentHashMap<>();

    public CaptchaServiceImpl(RsaKeyHolder rsaKeyHolder) {
        this.rsaKeyHolder = rsaKeyHolder;
    }

    @Override
    public CaptchaResp createCaptcha() {
        purgeExpired();
        String code = randomCode();
        String cid = UUID.randomUUID().toString().replace("-", "");
        store.put(cid, new CaptchaEntry(code.toLowerCase(), System.currentTimeMillis() + TTL_MS));
        String imageBase64 = renderImageBase64(code);
        logger.debug("验证码已生成: cid={}", cid);
        return new CaptchaResp(cid, rsaKeyHolder.getPublicKeyPem(), imageBase64);
    }

    @Override
    public void verifyAndConsume(String cid, String captchaCode) {
        if (cid == null || cid.trim().isEmpty() || captchaCode == null || captchaCode.trim().isEmpty()) {
            throw new BusinessException(UserErrorCode.CAPTCHA_INVALID);
        }
        CaptchaEntry entry = store.remove(cid.trim());
        if (entry == null || entry.expireAt < System.currentTimeMillis()) {
            throw new BusinessException(UserErrorCode.CAPTCHA_INVALID);
        }
        if (!entry.answer.equalsIgnoreCase(captchaCode.trim())) {
            throw new BusinessException(UserErrorCode.CAPTCHA_INVALID);
        }
    }

    @Override
    public String createCaptchaForTest(String answer) {
        if (answer == null || answer.trim().isEmpty()) {
            throw new IllegalArgumentException("test captcha answer required");
        }
        String cid = UUID.randomUUID().toString().replace("-", "");
        store.put(cid, new CaptchaEntry(answer.trim().toLowerCase(), System.currentTimeMillis() + TTL_MS));
        return cid;
    }

    private String randomCode() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        char[] buf = new char[CODE_LEN];
        for (int i = 0; i < CODE_LEN; i++) {
            buf[i] = CODE_CHARS[random.nextInt(CODE_CHARS.length)];
        }
        return new String(buf);
    }

    private String renderImageBase64(String code) {
        int width = 120;
        int height = 40;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(245, 247, 250));
            g.fillRect(0, 0, width, height);

            ThreadLocalRandom random = ThreadLocalRandom.current();
            for (int i = 0; i < 6; i++) {
                g.setColor(new Color(180 + random.nextInt(50), 180 + random.nextInt(50), 180 + random.nextInt(50)));
                g.drawLine(random.nextInt(width), random.nextInt(height),
                        random.nextInt(width), random.nextInt(height));
            }

            // 使用逻辑字体，避免 Alpine 容器无 Arial 导致画图失败
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
            int charWidth = width / (code.length() + 1);
            for (int i = 0; i < code.length(); i++) {
                g.setColor(new Color(30 + random.nextInt(100), 30 + random.nextInt(100), 30 + random.nextInt(100)));
                int x = charWidth * (i + 1) - 8;
                int y = 28 + random.nextInt(6);
                g.drawString(String.valueOf(code.charAt(i)), x, y);
            }
        } finally {
            g.dispose();
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("验证码图片生成失败", e);
        }
    }

    private void purgeExpired() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, CaptchaEntry>> it = store.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, CaptchaEntry> entry = it.next();
            if (entry.getValue().expireAt < now) {
                it.remove();
            }
        }
    }

    private static final class CaptchaEntry {
        private final String answer;
        private final long expireAt;

        private CaptchaEntry(String answer, long expireAt) {
            this.answer = answer;
            this.expireAt = expireAt;
        }
    }
}
