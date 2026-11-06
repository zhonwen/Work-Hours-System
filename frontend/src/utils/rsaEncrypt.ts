import JSEncrypt from 'jsencrypt'

/**
 * 使用服务端下发的 RSA 公钥加密明文（与后端 RSA/ECB/PKCS1Padding 对齐）
 */
export function encryptWithPublicKey(publicKeyPem: string, plainText: string): string {
  const encryptor = new JSEncrypt()
  encryptor.setPublicKey(publicKeyPem)
  const cipher = encryptor.encrypt(plainText)
  if (!cipher) {
    throw new Error('密码加密失败，请刷新验证码后重试')
  }
  return cipher
}

/**
 * 加密「明文密码|cid」，与登录/注册/改密/重置口径一致
 */
export function encryptPasswordWithCid(
  publicKeyPem: string,
  plainPassword: string,
  cid: string
): string {
  return encryptWithPublicKey(publicKeyPem, `${plainPassword}|${cid}`)
}
