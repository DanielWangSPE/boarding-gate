package com.boardinggate.auth.store;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * Redis 版加密材料存储。
 * <p>
 * Key 格式遵循 spec §1：{@code auth:crypto:login:{cryptoId}}，TTL 与 {@code ttlSeconds} 一致。
 * 通过 {@link StringRedisTemplate#delete(Object)} 的返回值保证"取出并作废"的原子性（单次消费）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.auth.crypto-store",
        name = "type",
        havingValue = "redis",
        matchIfMissing = true)
public class RedisCryptoMaterialStore implements CryptoMaterialStore {

    public static final String KEY_PREFIX = "auth:crypto:login:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void put(CryptoMaterial material) {
        long ttlMs = material.getExpiresAt() - Instant.now().toEpochMilli();
        if (ttlMs <= 0) {
            return;
        }
        try {
            String value = objectMapper.writeValueAsString(MaterialPayload.of(material));
            redis.opsForValue().set(KEY_PREFIX + material.getCryptoId(), value, Duration.ofMillis(ttlMs));
        } catch (Exception e) {
            log.error("写入 Redis 加密材料失败, cryptoId={}", material.getCryptoId(), e);
            throw new IllegalStateException("写入加密材料失败", e);
        }
    }

    @Override
    public CryptoMaterial consume(String cryptoId) {
        if (cryptoId == null || cryptoId.isEmpty()) {
            return null;
        }
        String key = KEY_PREFIX + cryptoId;
        String raw = redis.opsForValue().get(key);
        if (raw == null) {
            return null;
        }
        //  单次消费：先删除再解析；删除失败（可能已被并发消费）直接视为不存在
        Boolean deleted = redis.delete(key);
        if (deleted == null || !deleted) {
            return null;
        }
        try {
            MaterialPayload payload = objectMapper.readValue(raw, MaterialPayload.class);
            if (payload.expiresAt <= Instant.now().toEpochMilli()) {
                return null;
            }
            return new CryptoMaterial(
                    payload.cryptoId,
                    payload.algorithm,
                    payload.mode,
                    Base64.getDecoder().decode(payload.keyB64),
                    Base64.getDecoder().decode(payload.ivB64),
                    payload.expiresAt);
        } catch (Exception e) {
            log.warn("解析 Redis 加密材料失败, cryptoId={}", cryptoId, e);
            return null;
        }
    }

    /** Redis 内存储的序列化结构（key/iv 以 Base64 编码，避免二进制字节问题）。 */
    private static class MaterialPayload {
        public String cryptoId;
        public String algorithm;
        public String mode;
        public String keyB64;
        public String ivB64;
        public long expiresAt;

        public MaterialPayload() {
        }

        @JsonCreator
        public MaterialPayload(@JsonProperty("cryptoId") String cryptoId,
                               @JsonProperty("algorithm") String algorithm,
                               @JsonProperty("mode") String mode,
                               @JsonProperty("keyB64") String keyB64,
                               @JsonProperty("ivB64") String ivB64,
                               @JsonProperty("expiresAt") long expiresAt) {
            this.cryptoId = cryptoId;
            this.algorithm = algorithm;
            this.mode = mode;
            this.keyB64 = keyB64;
            this.ivB64 = ivB64;
            this.expiresAt = expiresAt;
        }

        static MaterialPayload of(CryptoMaterial m) {
            return new MaterialPayload(
                    m.getCryptoId(),
                    m.getAlgorithm(),
                    m.getMode(),
                    Base64.getEncoder().encodeToString(m.getKey()),
                    Base64.getEncoder().encodeToString(m.getIv()),
                    m.getExpiresAt());
        }
    }
}
