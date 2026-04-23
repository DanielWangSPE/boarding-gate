package com.boardinggate.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/**
 * JWT Token 签发与解析服务（RS256）。
 * <p>
 * 对应 spec §4.3：
 * <ul>
 *   <li>签名算法：RS256（非对称）</li>
 *   <li>Access Token Payload：uid、username、sessionId、jti、tokenType=access、iat、exp</li>
 *   <li>Refresh Token Payload：uid、sessionId、jti、tokenType=refresh、iat、exp</li>
 * </ul>
 * 若 classpath 下未提供 PEM 文件，启动时会自动生成一对临时 RSA 密钥（仅用于本地联调）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService implements InitializingBean {

    public static final String CLAIM_UID = "uid";
    public static final String CLAIM_USERNAME = "username";
    public static final String CLAIM_SESSION_ID = "sessionId";
    public static final String CLAIM_TOKEN_TYPE = "tokenType";
    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";

    private final AuthProperties authProperties;
    private final ResourceLoader resourceLoader;

    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;

    @Override
    public void afterPropertiesSet() throws Exception {
        this.privateKey = loadPrivateKey();
        this.publicKey = loadPublicKey();
        if (this.privateKey == null || this.publicKey == null) {
            log.warn("未找到 JWT 密钥文件，自动生成临时 RSA 密钥对（仅用于本地联调，生产必须改回固定密钥）");
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            KeyPair pair = kpg.generateKeyPair();
            this.privateKey = (RSAPrivateKey) pair.getPrivate();
            this.publicKey = (RSAPublicKey) pair.getPublic();
        }
    }

    public TokenPair issue(Long uid, String username, String sessionId) {
        Instant now = Instant.now();
        Date iat = Date.from(now);

        AuthProperties.Jwt jwtCfg = authProperties.getJwt();
        String accessJti = UUID.randomUUID().toString();
        String refreshJti = UUID.randomUUID().toString();

        String access = Jwts.builder()
                .setIssuer(jwtCfg.getIssuer())
                .setSubject(String.valueOf(uid))
                .setId(accessJti)
                .setIssuedAt(iat)
                .setExpiration(Date.from(now.plusSeconds(jwtCfg.getAccessTtlSeconds())))
                .claim(CLAIM_UID, uid)
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_SESSION_ID, sessionId)
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS)
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();

        String refresh = Jwts.builder()
                .setIssuer(jwtCfg.getIssuer())
                .setSubject(String.valueOf(uid))
                .setId(refreshJti)
                .setIssuedAt(iat)
                .setExpiration(Date.from(now.plusSeconds(jwtCfg.getRefreshTtlSeconds())))
                .claim(CLAIM_UID, uid)
                .claim(CLAIM_SESSION_ID, sessionId)
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_REFRESH)
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();

        return new TokenPair(access, accessJti, refresh, refreshJti,
                jwtCfg.getAccessTtlSeconds(), jwtCfg.getRefreshTtlSeconds());
    }

    public RSAPublicKey getPublicKey() {
        return publicKey;
    }

    /**
     * 解析并校验 JWT。校验签名、exp、iss 及 tokenType；任何失败抛 {@link JwtException}。
     *
     * @param token            原始 token 字符串
     * @param expectedTokenType "access" 或 "refresh"
     */
    public Claims parseAndVerify(String token, String expectedTokenType) {
        Jws<Claims> jws = Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .requireIssuer(authProperties.getJwt().getIssuer())
                .build()
                .parseClaimsJws(token);
        Claims claims = jws.getBody();
        String actual = claims.get(CLAIM_TOKEN_TYPE, String.class);
        if (!expectedTokenType.equals(actual)) {
            throw new JwtException("Token 类型不匹配，expected=" + expectedTokenType + " actual=" + actual);
        }
        return claims;
    }

    /** 返回给定 claims 距离过期还剩余的秒数，最少为 0。 */
    public long remainingSeconds(Claims claims) {
        Date exp = claims.getExpiration();
        if (exp == null) {
            return 0L;
        }
        long remain = (exp.getTime() - System.currentTimeMillis()) / 1000L;
        return Math.max(remain, 0L);
    }

    private RSAPrivateKey loadPrivateKey() {
        AuthProperties.Jwt jwtCfg = authProperties.getJwt();
        String pem = readPemIfExists(jwtCfg.getPrivateKeyLocation());
        if (pem == null) {
            return null;
        }
        try {
            byte[] der = Base64.getDecoder().decode(stripPemHeaders(pem));
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PrivateKey key = kf.generatePrivate(new PKCS8EncodedKeySpec(der));
            return (RSAPrivateKey) key;
        } catch (Exception e) {
            log.error("解析 JWT 私钥失败: {}", jwtCfg.getPrivateKeyLocation(), e);
            return null;
        }
    }

    private RSAPublicKey loadPublicKey() {
        AuthProperties.Jwt jwtCfg = authProperties.getJwt();
        String pem = readPemIfExists(jwtCfg.getPublicKeyLocation());
        if (pem == null) {
            return null;
        }
        try {
            byte[] der = Base64.getDecoder().decode(stripPemHeaders(pem));
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PublicKey key = kf.generatePublic(new X509EncodedKeySpec(der));
            return (RSAPublicKey) key;
        } catch (Exception e) {
            log.error("解析 JWT 公钥失败: {}", jwtCfg.getPublicKeyLocation(), e);
            return null;
        }
    }

    private String readPemIfExists(String location) {
        try {
            Resource res = resourceLoader.getResource(location);
            if (!res.exists()) {
                return null;
            }
            try (InputStream in = res.getInputStream()) {
                byte[] bytes = new byte[in.available()];
                int read = in.read(bytes);
                if (read <= 0) {
                    return null;
                }
                return new String(bytes, 0, read, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private String stripPemHeaders(String pem) {
        return pem.replaceAll("-----BEGIN [^-]+-----", "")
                .replaceAll("-----END [^-]+-----", "")
                .replaceAll("\\s+", "");
    }

    /** 一次签发的 Access + Refresh 对。 */
    @lombok.Getter
    @lombok.AllArgsConstructor
    public static class TokenPair {
        private final String accessToken;
        private final String accessJti;
        private final String refreshToken;
        private final String refreshJti;
        private final int accessTtlSeconds;
        private final int refreshTtlSeconds;
    }
}
