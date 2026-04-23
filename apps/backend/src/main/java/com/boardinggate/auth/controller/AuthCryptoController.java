package com.boardinggate.auth.controller;

import com.boardinggate.auth.dto.CryptoParamsResp;
import com.boardinggate.auth.dto.LoginReq;
import com.boardinggate.auth.dto.LoginResp;
import com.boardinggate.auth.service.AuthService;
import com.boardinggate.auth.service.CryptoService;
import com.boardinggate.web.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

/**
 * 登录相关接口：
 * <ul>
 *   <li>GET  /auth/crypto/login - 获取 SM4 加密参数</li>
 *   <li>POST /auth/crypto/login - 账号密码登录</li>
 * </ul>
 * <p>
 * 基础路径（context-path）由 application.yml 的 server.servlet.context-path=/api 提供，
 * 实际对外 URL 为 /api/auth/crypto/login。
 */
@RestController
@RequestMapping("/auth/crypto")
@RequiredArgsConstructor
public class AuthCryptoController {

    private final CryptoService cryptoService;
    private final AuthService authService;

    /** §2.1 获取登录 SM4 加密参数 */
    @GetMapping("/login")
    public ApiResponse<CryptoParamsResp> issueCryptoParams() {
        return ApiResponse.success(cryptoService.issue());
    }

    /** §2.2 账号密码登录 */
    @PostMapping("/login")
    public ApiResponse<LoginResp> login(@Valid @RequestBody LoginReq req,
                                        HttpServletRequest httpReq,
                                        HttpServletResponse httpResp) {
        return ApiResponse.success(authService.login(req, httpReq, httpResp));
    }
}
