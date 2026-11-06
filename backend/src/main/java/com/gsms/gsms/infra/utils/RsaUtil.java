package com.gsms.gsms.infra.utils;

import com.gsms.gsms.infra.exception.BusinessException;
import com.gsms.gsms.model.enums.errorcode.UserErrorCode;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.Base64;

/**
 * RSA 加解密工具（与前端 jsencrypt 默认 PKCS#1 v1.5 对齐）
 */
@Component
public class RsaUtil {

    private static final String TRANSFORMATION = "RSA/ECB/PKCS1Padding";

    private final RsaKeyHolder rsaKeyHolder;

    public RsaUtil(RsaKeyHolder rsaKeyHolder) {
        this.rsaKeyHolder = rsaKeyHolder;
    }

    /**
     * 使用服务端私钥解密前端密文（Base64）
     */
    public String decrypt(String cipherBase64) {
        if (cipherBase64 == null || cipherBase64.trim().isEmpty()) {
            throw new BusinessException(UserErrorCode.LOGIN_DECRYPT_FAILED);
        }
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, rsaKeyHolder.getPrivateKey());
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(cipherBase64.trim()));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new BusinessException(UserErrorCode.LOGIN_DECRYPT_FAILED);
        }
    }

    /**
     * 解密「明文密码|cid」并校验与请求 cid 一致，返回明文密码
     */
    public String decryptPasswordBoundToCid(String cipherBase64, String expectedCid) {
        if (expectedCid == null || expectedCid.trim().isEmpty()) {
            throw new BusinessException(UserErrorCode.LOGIN_CID_MISMATCH);
        }
        String plain = decrypt(cipherBase64);
        int sep = plain.lastIndexOf('|');
        if (sep <= 0 || sep >= plain.length() - 1) {
            throw new BusinessException(UserErrorCode.LOGIN_DECRYPT_FAILED);
        }
        String plainPassword = plain.substring(0, sep);
        String cidFromCipher = plain.substring(sep + 1);
        if (!expectedCid.equals(cidFromCipher)) {
            throw new BusinessException(UserErrorCode.LOGIN_CID_MISMATCH);
        }
        return plainPassword;
    }

    /**
     * 使用公钥加密（测试 / 联调用）
     */
    public String encryptWithPublicKey(String plainText) {
        try {
            return encrypt(plainText, rsaKeyHolder.getPublicKey());
        } catch (Exception e) {
            throw new IllegalStateException("RSA 加密失败", e);
        }
    }

    public static String encrypt(String plainText, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }
}
