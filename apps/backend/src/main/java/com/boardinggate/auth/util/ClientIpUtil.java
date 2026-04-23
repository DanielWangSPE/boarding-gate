package com.boardinggate.auth.util;

import javax.servlet.http.HttpServletRequest;
import java.util.Objects;

public final class ClientIpUtil {

    private static final String[] HEADERS = {
            "X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP"
    };

    private ClientIpUtil() {
    }

    public static String resolve(HttpServletRequest req) {
        for (String h : HEADERS) {
            String v = req.getHeader(h);
            if (v != null && !v.isEmpty() && !"unknown".equalsIgnoreCase(v)) {
                int comma = v.indexOf(',');
                return comma > -1 ? v.substring(0, comma).trim() : v.trim();
            }
        }
        return Objects.toString(req.getRemoteAddr(), "");
    }
}
