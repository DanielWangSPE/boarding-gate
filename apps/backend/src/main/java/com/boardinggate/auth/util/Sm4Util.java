package com.boardinggate.auth.util;

import cn.hutool.crypto.Mode;
import cn.hutool.crypto.Padding;
import cn.hutool.crypto.symmetric.SM4;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * SM4（国密）对称加解密工具。
 * <p>
 * 与 spec §4.1 保持一致：
 * <ul>
 *   <li>分组模式：CBC / PKCS7 填充</li>
 *   <li>密钥 16 字节、IV 16 字节</li>
 *   <li>密文使用 <b>Hex（十六进制字符串）</b>表示</li>
 *   <li>key / iv 在 GET 响应中使用 Base64</li>
 * </ul>
 */
public final class Sm4Util {

    public static final int KEY_LEN = 16;
    public static final int IV_LEN = 16;
    public static final String ALGORITHM = "SM4";
    public static final String MODE_CBC = "CBC";

    private static final SecureRandom RANDOM = new SecureRandom();

    private Sm4Util() {
    }

    /** 生成 16 字节密钥。 */
    public static byte[] generateKey() {
        byte[] key = new byte[KEY_LEN];
        RANDOM.nextBytes(key);
        return key;
    }

    /** 生成 16 字节 IV。 */
    public static byte[] generateIv() {
        byte[] iv = new byte[IV_LEN];
        RANDOM.nextBytes(iv);
        return iv;
    }

    /**
     * 解密 SM4-CBC/PKCS7 的 Hex 密文，返回 UTF-8 明文。
     *
     * @param cipherHex 密文 Hex 字符串（前端 sm-crypto 默认输出格式）
     * @param key       16 字节密钥
     * @param iv        16 字节 IV
     * @return 明文
     */
    public static String decryptFromHex(String cipherHex, byte[] key, byte[] iv) {
        if (cipherHex == null || cipherHex.isEmpty()) {
            throw new IllegalArgumentException("cipherHex 不能为空");
        }
        byte[] cipherBytes = hexToBytes(cipherHex);
        SM4 sm4 = new SM4(Mode.CBC, Padding.PKCS5Padding, key, iv);
        byte[] plain = sm4.decrypt(cipherBytes);
        return new String(plain, StandardCharsets.UTF_8);
    }

    private static byte[] hexToBytes(String hex) {
        String s = hex.trim();
        if ((s.length() & 1) != 0) {
            throw new IllegalArgumentException("Hex 字符串长度必须为偶数");
        }
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int hi = Character.digit(s.charAt(i), 16);
            int lo = Character.digit(s.charAt(i + 1), 16);
            if (hi < 0 || lo < 0) {
                throw new IllegalArgumentException("非法 Hex 字符");
            }
            data[i / 2] = (byte) ((hi << 4) | lo);
        }
        return data;
    }
}
