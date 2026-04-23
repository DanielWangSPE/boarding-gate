package com.boardinggate.auth.service;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 认证相关配置（application.yml 中 app.auth.*）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

    /** SM4 加密材料有效期（秒） */
    private int cryptoTtlSeconds = 180;

    private Jwt jwt = new Jwt();

    private RefreshCookie refreshCookie = new RefreshCookie();

    @Data
    public static class Jwt {
        private String issuer = "boarding-gate";
        private int accessTtlSeconds = 1800;
        private int refreshTtlSeconds = 1209600;
        private String privateKeyLocation = "classpath:keys/jwt-private.pem";
        private String publicKeyLocation = "classpath:keys/jwt-public.pem";
    }

    @Data
    public static class RefreshCookie {
        private String name = "refreshToken";
        private String path = "/api/auth/refresh";
        private String sameSite = "Strict";
        private boolean secure = false;
        private boolean httpOnly = true;
    }
}
