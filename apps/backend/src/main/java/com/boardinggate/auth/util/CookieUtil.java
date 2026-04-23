package com.boardinggate.auth.util;

import com.boardinggate.auth.service.AuthProperties;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Refresh Token Cookie 读写工具。
 * <p>
 * 由于 Servlet 3.x 的 {@link javax.servlet.http.Cookie} 不支持 {@code SameSite} 属性，
 * 这里手动拼装 {@code Set-Cookie} 响应头。
 */
public final class CookieUtil {

    private CookieUtil() {
    }

    public static void writeRefresh(HttpServletResponse resp, AuthProperties.RefreshCookie cfg,
                                    String value, int maxAgeSeconds) {
        resp.addHeader("Set-Cookie", buildHeader(cfg, value, maxAgeSeconds));
    }

    /** 清除 Cookie：写入空值 + Max-Age=0 指示浏览器立即删除。 */
    public static void clearRefresh(HttpServletResponse resp, AuthProperties.RefreshCookie cfg) {
        resp.addHeader("Set-Cookie", buildHeader(cfg, "", 0));
    }

    public static String readRefresh(HttpServletRequest req, AuthProperties.RefreshCookie cfg) {
        if (req.getCookies() == null) {
            return null;
        }
        for (Cookie c : req.getCookies()) {
            if (cfg.getName().equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }

    private static String buildHeader(AuthProperties.RefreshCookie cfg, String value, int maxAge) {
        StringBuilder sb = new StringBuilder(256);
        sb.append(cfg.getName()).append('=').append(value);
        sb.append("; Path=").append(cfg.getPath());
        sb.append("; Max-Age=").append(maxAge);
        if (cfg.isHttpOnly()) {
            sb.append("; HttpOnly");
        }
        if (cfg.isSecure()) {
            sb.append("; Secure");
        }
        if (cfg.getSameSite() != null && !cfg.getSameSite().isEmpty()) {
            sb.append("; SameSite=").append(cfg.getSameSite());
        }
        return sb.toString();
    }
}
