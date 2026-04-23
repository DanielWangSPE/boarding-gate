package com.boardinggate.auth.store;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 会话吊销标记（Redis）。
 * <p>
 * 会话吊销以 {@code sessionId} 为粒度：一旦吊销，同一会话签发的任何 Access / Refresh Token
 * 即使 JWT 本身尚未过期，也应被服务端判定为失效。
 * <pre>
 * Key:   auth:session:revoked:{sessionId}
 * TTL:   与 Refresh Token 的剩余有效期一致，到期后 Redis 自动清理
 * </pre>
 */
@Component
@RequiredArgsConstructor
public class SessionRevocationStore {

    public static final String KEY_PREFIX = "auth:session:revoked:";

    private final StringRedisTemplate redis;

    public void revoke(String sessionId, long ttlSeconds) {
        if (sessionId == null || sessionId.isEmpty() || ttlSeconds <= 0) {
            return;
        }
        redis.opsForValue().set(KEY_PREFIX + sessionId, "1", Duration.ofSeconds(ttlSeconds));
    }

    public boolean isRevoked(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return false;
        }
        Boolean has = redis.hasKey(KEY_PREFIX + sessionId);
        return Boolean.TRUE.equals(has);
    }
}
