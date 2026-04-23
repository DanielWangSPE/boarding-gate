package com.boardinggate.user.controller;

import com.boardinggate.user.entity.SysUser;
import com.boardinggate.user.service.SysUserService;
import com.boardinggate.web.dto.ApiResponse;
import com.boardinggate.web.security.CurrentUser;
import com.boardinggate.web.security.LoginUser;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 示例：受保护的业务接口。
 * <p>
 * 本接口不在 {@code JwtAuthFilter} 白名单中，因此进入 Controller 时已经可以安全假设：
 * <ul>
 *   <li>请求携带了有效 Access Token（RS256 签名正确、未过期、tokenType=access）</li>
 *   <li>对应会话未被吊销</li>
 *   <li>{@link LoginUser} 已通过 {@link CurrentUser} 注入</li>
 * </ul>
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final SysUserService sysUserService;

    /** 获取当前登录用户基本信息。 */
    @GetMapping("/me")
    public ApiResponse<MeResp> me(@CurrentUser LoginUser loginUser) {
        SysUser user = sysUserService.findByUsername(loginUser.getUsername());
        MeResp resp = new MeResp();
        resp.setUserId(loginUser.getUserId());
        resp.setUsername(loginUser.getUsername());
        resp.setSessionId(loginUser.getSessionId());
        if (user != null) {
            resp.setNickname(user.getNickname());
            resp.setStatus(user.getStatus());
        }
        return ApiResponse.success(resp);
    }

    @Data
    public static class MeResp {
        private Long userId;
        private String username;
        private String nickname;
        private Integer status;
        private String sessionId;
    }
}
