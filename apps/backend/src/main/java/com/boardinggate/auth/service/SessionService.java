package com.boardinggate.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boardinggate.auth.entity.SysSession;
import com.boardinggate.auth.mapper.SysSessionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final SysSessionMapper sysSessionMapper;

    public SysSession create(Long userId, String username, String loginIp, String userAgent,
                             int refreshTtlSeconds) {
        SysSession session = new SysSession();
        session.setSessionId(UUID.randomUUID().toString().replace("-", ""));
        session.setUserId(userId);
        session.setUsername(username);
        session.setLoginIp(loginIp);
        session.setUserAgent(userAgent);
        session.setStatus(1);
        session.setExpiresAt(LocalDateTime.now().plusSeconds(refreshTtlSeconds));
        sysSessionMapper.insert(session);
        return session;
    }

    public void bindTokens(SysSession session, String accessJti, String refreshJti) {
        session.setAccessJti(accessJti);
        session.setRefreshJti(refreshJti);
        sysSessionMapper.updateById(session);
    }

    public SysSession findBySessionId(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return null;
        }
        LambdaQueryWrapper<SysSession> qw = new LambdaQueryWrapper<>();
        qw.eq(SysSession::getSessionId, sessionId);
        qw.last("LIMIT 1");
        return sysSessionMapper.selectOne(qw);
    }

    /**
     * 将会话置为失效（status=0）。不删除记录，保留审计线索。
     */
    public void revoke(SysSession session) {
        if (session == null) {
            return;
        }
        session.setStatus(0);
        session.setExpiresAt(LocalDateTime.now());
        sysSessionMapper.updateById(session);
    }

    /**
     * 刷新流程中旋转 Token（更新 access_jti / refresh_jti）并延长过期时刻。
     */
    public void rotateTokens(SysSession session, String newAccessJti, String newRefreshJti,
                             int refreshTtlSeconds) {
        session.setAccessJti(newAccessJti);
        session.setRefreshJti(newRefreshJti);
        session.setExpiresAt(LocalDateTime.now().plusSeconds(refreshTtlSeconds));
        sysSessionMapper.updateById(session);
    }
}
