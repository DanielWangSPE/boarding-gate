/**
 * 国密 SM4 加解密工具（前端侧）。
 *
 * 后端 spec §4.1 约定：
 *   - 算法：SM4-CBC
 *   - 填充：PKCS7
 *   - Key / IV：16 字节随机，通过 /auth/crypto/login 接口由后端颁发，Base64 编码
 *   - 密文：Hex（小写）
 *
 * 本模块仅做"加密 + 编码"的薄封装，不缓存任何密钥材料（单次请求单次消费）。
 */
import { sm4 } from 'sm-crypto';

/**
 * 将 Base64 字符串解码为 Hex 字符串。
 * sm-crypto 的 sm4.encrypt 只接受 Hex 形式的 key / iv。
 */
function base64ToHex(base64: string): string {
  const binary = atob(base64);
  let hex = '';
  for (let i = 0; i < binary.length; i++) {
    //  atob 产出的每个"字符"都在 0-255 范围内，单码元即完整码点，codePointAt 安全。
    const byte = (binary.codePointAt(i) ?? 0).toString(16);
    hex += byte.padStart(2, '0');
  }
  return hex;
}

/**
 * 使用后端下发的 key/iv 对明文密码做 SM4-CBC/PKCS7 加密，输出 Hex 密文。
 *
 * @param plain      明文密码（UTF-8）
 * @param keyBase64  Base64 编码的 16 字节 Key
 * @param ivBase64   Base64 编码的 16 字节 IV
 */
export function encryptPasswordBySm4(
  plain: string,
  keyBase64: string,
  ivBase64: string,
): string {
  const keyHex = base64ToHex(keyBase64);
  const ivHex = base64ToHex(ivBase64);
  return sm4.encrypt(plain, keyHex, {
    mode: 'cbc',
    iv: ivHex,
    padding: 'pkcs#7',
    output: 'string',
  });
}
