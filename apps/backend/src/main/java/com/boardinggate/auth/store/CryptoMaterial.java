package com.boardinggate.auth.store;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

/**
 * 一次性 SM4 密钥材料。
 */
@Getter
@AllArgsConstructor
public class CryptoMaterial {

    private final String cryptoId;
    private final String algorithm;   // 固定 SM4
    private final String mode;        // CBC
    private final byte[] key;
    private final byte[] iv;
    /** 过期时间（epoch millis） */
    private final long expiresAt;

    public boolean isExpired() {
        return Instant.now().toEpochMilli() > expiresAt;
    }
}
