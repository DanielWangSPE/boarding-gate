package com.boardinggate.auth.dto;

import lombok.Builder;
import lombok.Data;

/**
 * GET /auth/crypto/login 响应体。
 */
@Data
@Builder
public class CryptoParamsResp {

    private String cryptoId;
    private String algorithm;
    private String mode;
    /** Base64(16-byte-key) */
    private String key;
    /** Base64(16-byte-iv) */
    private String iv;
    private Integer ttlSeconds;
}
