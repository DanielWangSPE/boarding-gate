package com.boardinggate.auth.service;

import com.boardinggate.auth.dto.LoginReq;
import com.boardinggate.auth.dto.LoginResp;
import com.boardinggate.auth.entity.SysSession;
import com.boardinggate.auth.util.ClientIpUtil;
import com.boardinggate.auth.util.CookieUtil;
import com.boardinggate.user.entity.SysUser;
import com.boardinggate.user.service.SysUserService;
import com.boardinggate.web.dto.ApiCode;
import com.boardinggate.web.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 登录认证主业务流水。实现 spec §3 中列出的 9 个步骤。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final CryptoService cryptoService;
    private final SysUserService sysUserService;
    private final SessionService sessionService;
    private final TokenService tokenService;
    private final LoginLogService loginLogService;
    private final AuthProperties authProperties;

    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder();

    public LoginResp login(LoginReq req, HttpServletRequest httpReq, HttpServletResponse httpResp) {
        String loginIp = ClientIpUtil.resolve(httpReq);
        String userAgent = httpReq.getHeader("User-Agent");

        //  步骤 2：解析并单次消费 cryptoId，SM4 解密密文得到明文密码
        String plainPassword = cryptoService.consumeAndDecrypt(req.getCryptoId(), req.getPassword());

        //  步骤 3：查询用户
        SysUser user = sysUserService.findByUsername(req.getUsername());
        if (user == null) {
            loginLogService.recordFailure(req.getUsername(), null, loginIp, userAgent, ApiCode.LOGIN_BAD_CREDENTIALS.getCode());
            // 统一提示"用户名或密码错误"，不区分是否存在
            throw new BusinessException(ApiCode.LOGIN_BAD_CREDENTIALS);
        }

        //  账号状态校验
        if (!user.isEnabled()) {
            loginLogService.recordFailure(req.getUsername(), user.getId(), loginIp, userAgent, ApiCode.LOGIN_ACCOUNT_DISABLED.getCode());
            throw new BusinessException(ApiCode.LOGIN_ACCOUNT_DISABLED);
        }

        //  步骤 5：BCrypt 校验
        if (!BCRYPT.matches(plainPassword, user.getPasswordHash())) {
            loginLogService.recordFailure(req.getUsername(), user.getId(), loginIp, userAgent, ApiCode.LOGIN_BAD_CREDENTIALS.getCode());
            throw new BusinessException(ApiCode.LOGIN_BAD_CREDENTIALS);
        }

        //  步骤 6：创建会话
        SysSession session = sessionService.create(
                user.getId(), user.getUsername(), loginIp, userAgent,
                authProperties.getJwt().getRefreshTtlSeconds());

        //  步骤 8：签发 Token 并回写会话
        TokenService.TokenPair tokens = tokenService.issue(user.getId(), user.getUsername(), session.getSessionId());
        sessionService.bindTokens(session, tokens.getAccessJti(), tokens.getRefreshJti());

        //  更新用户最后登录信息
        sysUserService.markLoginSuccess(user.getId(), loginIp);

        //  步骤 9：异步写登录日志
        loginLogService.recordSuccess(user.getUsername(), user.getId(), loginIp, userAgent, session.getSessionId());

        //  写入 Refresh Token HttpOnly Cookie
        CookieUtil.writeRefresh(httpResp, authProperties.getRefreshCookie(),
                tokens.getRefreshToken(), tokens.getRefreshTtlSeconds());

        //  步骤 7：判断是否需要强制修改密码
        boolean forceChange = user.isForceChangePassword();
        LoginResp body = LoginResp.builder()
                .accessToken(tokens.getAccessToken())
                .expiresIn(tokens.getAccessTtlSeconds())
                .tokenType("Bearer")
                .forceChangePassword(forceChange)
                .build();

        if (forceChange) {
            //  按 spec §2.2 返回 A0220，data 中仍携带 accessToken，供前端进入改密流程
            throw new BusinessException(ApiCode.LOGIN_FORCE_CHANGE_PASSWORD, body);
        }
        return body;
    }

}
