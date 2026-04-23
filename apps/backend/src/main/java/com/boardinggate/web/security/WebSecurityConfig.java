package com.boardinggate.web.security;

import com.boardinggate.auth.service.TokenService;
import com.boardinggate.auth.store.SessionRevocationStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * 注册自定义 Servlet Filter。
 * <p>
 * 选用原生 {@link FilterRegistrationBean} 而非 Spring Security，理由：
 * <ul>
 *   <li>项目未引入 {@code spring-boot-starter-security}，保持依赖最小化</li>
 *   <li>鉴权逻辑完全由 JWT + Redis 控制，无需 Security 的 AuthenticationManager / SecurityContext 体系</li>
 * </ul>
 */
@Configuration
public class WebSecurityConfig {

    @Bean
    public FilterRegistrationBean<JwtAuthFilter> jwtAuthFilterRegistration(
            TokenService tokenService,
            SessionRevocationStore revocationStore,
            ObjectMapper objectMapper) {
        FilterRegistrationBean<JwtAuthFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(new JwtAuthFilter(tokenService, revocationStore, objectMapper));
        reg.addUrlPatterns("/*");
        reg.setName("jwtAuthFilter");
        //  保证在业务请求之前尽早执行；数值越小越早，高于默认 MVC 过滤器
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 50);
        return reg;
    }
}
