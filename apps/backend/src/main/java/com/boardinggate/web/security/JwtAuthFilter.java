package com.boardinggate.web.security;

import com.boardinggate.auth.service.TokenService;
import com.boardinggate.auth.store.SessionRevocationStore;
import com.boardinggate.web.dto.ApiCode;
import com.boardinggate.web.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * Access Token 校验过滤器。
 * <p>
 * 在 Spring MVC 之前执行：白名单放行 → 解析 Bearer → 校验 JWT → 查 Redis 吊销标记 →
 * 向 {@link LoginUserContext} 写入当前用户；任何失败均统一以 {@link ApiResponse} 外壳响应
 * （HTTP 仍为 200，业务码 A0231/A0232/A0240 参考 spec）。
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    /** 完全不需要登录的路径（基于 servletPath，不含 context-path）。 */
    private static final List<String> WHITELIST = Arrays.asList(
            "/auth/crypto/login",
            "/auth/refresh",
            "/auth/logout",
            "/error",
            "/actuator/**",
            "/favicon.ico"
    );

    private static final PathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenService tokenService;
    private final SessionRevocationStore revocationStore;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        for (String pattern : WHITELIST) {
            if (PATH_MATCHER.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            writeError(response, ApiCode.UNAUTHENTICATED);
            return;
        }
        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            writeError(response, ApiCode.UNAUTHENTICATED);
            return;
        }

        Claims claims;
        try {
            claims = tokenService.parseAndVerify(token, TokenService.TOKEN_TYPE_ACCESS);
        } catch (JwtException e) {
            log.debug("Access Token 校验失败, uri={}, reason={}", request.getRequestURI(), e.getMessage());
            writeError(response, ApiCode.ACCESS_TOKEN_INVALID);
            return;
        }

        String sessionId = claims.get(TokenService.CLAIM_SESSION_ID, String.class);
        if (sessionId == null || sessionId.isEmpty()) {
            writeError(response, ApiCode.ACCESS_TOKEN_INVALID);
            return;
        }
        if (revocationStore.isRevoked(sessionId)) {
            writeError(response, ApiCode.REFRESH_TOKEN_REVOKED);
            return;
        }

        LoginUser user = new LoginUser(
                parseUid(claims),
                claims.get(TokenService.CLAIM_USERNAME, String.class),
                sessionId,
                claims.getId());
        LoginUserContext.set(user);
        try {
            chain.doFilter(request, response);
        } finally {
            LoginUserContext.clear();
        }
    }

    private Long parseUid(Claims claims) {
        try {
            String sub = claims.getSubject();
            if (sub != null && !sub.isEmpty()) {
                return Long.parseLong(sub);
            }
        } catch (NumberFormatException ignored) {
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

    private void writeError(HttpServletResponse resp, ApiCode code) throws IOException {
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType(MediaType.APPLICATION_JSON_VALUE);
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        ApiResponse<Void> body = ApiResponse.fail(code);
        resp.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
