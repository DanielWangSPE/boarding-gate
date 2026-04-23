package com.boardinggate.auth.service;

import com.boardinggate.auth.dto.CryptoParamsResp;
import com.boardinggate.auth.store.CryptoMaterial;
import com.boardinggate.auth.store.CryptoMaterialStore;
import com.boardinggate.auth.util.Sm4Util;
import com.boardinggate.web.dto.ApiCode;
import com.boardinggate.web.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * SM4 加密材料签发与一次性消费。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CryptoService {

    private final AuthProperties authProperties;
    private final CryptoMaterialStore store;

    public CryptoParamsResp issue() {
        try {
            byte[] key = Sm4Util.generateKey();
            byte[] iv = Sm4Util.generateIv();
            int ttl = authProperties.getCryptoTtlSeconds();
            String cryptoId = UUID.randomUUID().toString();
            long expiresAt = Instant.now().plusSeconds(ttl).toEpochMilli();

            CryptoMaterial material = new CryptoMaterial(
                    cryptoId, Sm4Util.ALGORITHM, Sm4Util.MODE_CBC, key, iv, expiresAt);
            store.put(material);

            return CryptoParamsResp.builder()
                    .cryptoId(cryptoId)
                    .algorithm(Sm4Util.ALGORITHM)
                    .mode(Sm4Util.MODE_CBC)
                    .key(Base64.getEncoder().encodeToString(key))
                    .iv(Base64.getEncoder().encodeToString(iv))
                    .ttlSeconds(ttl)
                    .build();
        } catch (Exception e) {
            log.error("签发 SM4 加密参数失败", e);
            throw new BusinessException(ApiCode.CRYPTO_ISSUE_FAILED);
        }
    }

    /**
     * 单次消费加密材料并解密密文。
     * <p>
     * 无论解密成功与否，材料均已从存储中移除，不可重用。
     *
     * @return 明文；解密失败抛 {@link BusinessException}(A0213)
     */
    public String consumeAndDecrypt(String cryptoId, String cipherHex) {
        CryptoMaterial material = store.consume(cryptoId);
        if (material == null) {
            throw new BusinessException(ApiCode.LOGIN_CRYPTO_INVALID);
        }
        try {
            return Sm4Util.decryptFromHex(cipherHex, material.getKey(), material.getIv());
        } catch (Exception e) {
            log.warn("SM4 解密失败, cryptoId={}", cryptoId);
            throw new BusinessException(ApiCode.LOGIN_CRYPTO_INVALID);
        }
    }
}
