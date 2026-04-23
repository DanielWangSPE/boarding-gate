package com.boardinggate.auth.controller;

import com.boardinggate.auth.dto.RefreshResp;
import com.boardinggate.auth.service.AuthSessionService;
import com.boardinggate.web.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 会话管理接口：
 * <ul>
 *   <li>POST /auth/refresh - 刷新 Access Token（旋转 Refresh Token）</li>
 *   <li>POST /auth/logout  - 登出，吊销当前会话</li>
 * </ul>
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthSessionController {

    private final AuthSessionService authSessionService;

    @PostMapping("/refresh")
    public ApiResponse<RefreshResp> refresh(HttpServletRequest req, HttpServletResponse resp) {
        return ApiResponse.success(authSessionService.refresh(req, resp));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest req, HttpServletResponse resp) {
        authSessionService.logout(req, resp);
        return ApiResponse.success(null);
    }
}
