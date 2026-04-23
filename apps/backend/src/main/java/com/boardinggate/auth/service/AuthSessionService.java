package com.boardinggate.auth.service;

import com.boardinggate.auth.dto.RefreshResp;
import com.boardinggate.auth.entity.SysSession;
import com.boardinggate.auth.store.SessionRevocationStore;
import com.boardinggate.auth.util.ClientIpUtil;
import com.boardinggate.auth.util.CookieUtil;
import com.boardinggate.web.dto.ApiCode;
import com.boardinggate.web.exception.BusinessException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 会话管理：刷新（rotate）、登出（revoke）。
 * <p>
 * Redis 中使用 {@link SessionRevocationStore} 标记已吊销的 sessionId，TTL 与 Refresh Token 剩余时间对齐；
 * 数据库中同步将 {@code sys_session.status} 置 0。Access Token 是否失效，需要资源服务器在鉴权时查询
 * Redis 吊销标记 + Session 表（本次不包含鉴权过滤器）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthSessionService {

    private final TokenService tokenService;
    private final SessionService sessionService;
    private final AuthProperties authProperties;
    private final SessionRevocationStore revocationStore;

    /**
     * 使用 Refresh Token（来自 HttpOnly Cookie）换取新的 Access + Refresh Token。
     * 成功后旧 Refresh 会被旋转作废（其 jti 不再等于 session.refresh_jti），防重放。
     */
    public RefreshResp refresh(HttpServletRequest req, HttpServletResponse resp) {
        String refreshToken = CookieUtil.readRefresh(req, authProperties.getRefreshCookie());
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new BusinessException(ApiCode.REFRESH_TOKEN_INVALID);
        }

        Claims claims;
        try {
            claims = tokenService.parseAndVerify(refreshToken, TokenService.TOKEN_TYPE_REFRESH);
        } catch (JwtException e) {
            log.debug("刷新失败：Refresh Token 解析/校验失败: {}", e.getMessage());
            throw new BusinessException(ApiCode.REFRESH_TOKEN_INVALID);
        }

        String sessionId = claims.get(TokenService.CLAIM_SESSION_ID, String.class);
        String jti = claims.getId();
        Long userId = parseUid(claims);

        //  1) Redis 吊销检查
        if (revocationStore.isRevoked(sessionId)) {
            throw new BusinessException(ApiCode.REFRESH_TOKEN_REVOKED);
        }

        //  2) 查库核对 session 状态与 jti 绑定（旋转后旧 jti 不再匹配 → 视为已作废）
        SysSession session = sessionService.findBySessionId(sessionId);
        if (session == null || session.getStatus() == null || session.getStatus() != 1) {
            throw new BusinessException(ApiCode.REFRESH_TOKEN_REVOKED);
        }
        if (!jti.equals(session.getRefreshJti())) {
            //  旧 Refresh 被重放或并发刷新，主动吊销整个会话
            log.warn("Refresh Token jti 与会话绑定不一致，可能重放攻击。sessionId={} tokenJti={} bindJti={}",
                    sessionId, jti, session.getRefreshJti());
            revokeSession(session);
            throw new BusinessException(ApiCode.REFRESH_TOKEN_REVOKED);
        }

        //  3) 签发新 Token 并旋转
        TokenService.TokenPair tokens = tokenService.issue(userId, session.getUsername(), sessionId);
        sessionService.rotateTokens(session, tokens.getAccessJti(), tokens.getRefreshJti(),
                authProperties.getJwt().getRefreshTtlSeconds());

        //  4) 写新 Cookie
        CookieUtil.writeRefresh(resp, authProperties.getRefreshCookie(),
                tokens.getRefreshToken(), tokens.getRefreshTtlSeconds());

        return RefreshResp.builder()
                .accessToken(tokens.getAccessToken())
                .expiresIn(tokens.getAccessTtlSeconds())
                .tokenType("Bearer")
                .build();
    }

    /**
     * 登出：优先以 Access Token 标识会话，其次回退到 Refresh Cookie。任何能标识会话的信息缺失时，
     * 仅清 Cookie 不抛异常（幂等性）。
     */
    public void logout(HttpServletRequest req, HttpServletResponse resp) {
        String sessionId = null;

        //  1) 尝试从 Authorization: Bearer 解析 access token
        String authHeader = req.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String accessToken = authHeader.substring(7).trim();
            try {
                Claims claims = tokenService.parseAndVerify(accessToken, TokenService.TOKEN_TYPE_ACCESS);
                sessionId = claims.get(TokenService.CLAIM_SESSION_ID, String.class);
            } catch (JwtException ignored) {
                //  access 可能已过期，尝试用 refresh cookie
            }
        }

        //  2) 回退：从 refresh cookie 取 sessionId
        if (sessionId == null) {
            String refreshToken = CookieUtil.readRefresh(req, authProperties.getRefreshCookie());
            if (refreshToken != null && !refreshToken.isEmpty()) {
                try {
                    Claims claims = tokenService.parseAndVerify(refreshToken, TokenService.TOKEN_TYPE_REFRESH);
                    sessionId = claims.get(TokenService.CLAIM_SESSION_ID, String.class);
                } catch (JwtException ignored) {
                    //  两者都解析不了：幂等结束
                }
            }
        }

        if (sessionId != null) {
            SysSession session = sessionService.findBySessionId(sessionId);
            if (session != null && session.getStatus() != null && session.getStatus() == 1) {
                log.info("用户登出：sessionId={}, username={}, ip={}",
                        sessionId, session.getUsername(), ClientIpUtil.resolve(req));
                revokeSession(session);
            }
        }

        //  3) 清 cookie（无论是否找到会话）
        CookieUtil.clearRefresh(resp, authProperties.getRefreshCookie());
    }

    private Long parseUid(Claims claims) {
        //  优先用 sub（登录时 setSubject(String.valueOf(uid))，更稳定）
        try {
            String sub = claims.getSubject();
            if (sub != null && !sub.isEmpty()) {
                return Long.parseLong(sub);
            }
        } catch (NumberFormatException ignored) {
            //  回退到 uid claim
        }
        Object raw = claims.get(TokenService.CLAIM_UID);
        if (raw instanceof Number) {
            return ((Number) raw).longValue();
        }
        if (raw != null) {
            try {
                return Long.parseLong(raw.toString());
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private void revokeSession(SysSession session) {
        sessionService.revoke(session);
        //  Redis 中保留吊销标记至少到 Refresh Token 原本的过期时刻
        revocationStore.revoke(session.getSessionId(),
                authProperties.getJwt().getRefreshTtlSeconds());
    }
}
