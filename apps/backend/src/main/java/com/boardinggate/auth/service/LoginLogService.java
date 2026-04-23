package com.boardinggate.auth.service;

import com.boardinggate.auth.entity.SysLoginLog;
import com.boardinggate.auth.mapper.SysLoginLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 登录日志服务（异步落库）。
 * <p>
 * 对应 spec §4.5：
 * <ul>
 *   <li>登录成功和失败均需记录</li>
 *   <li>异步记录，不阻塞主流程</li>
 *   <li>字段：username、loginTime、loginIp、userAgent、loginResult、failReason</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginLogService {

    private final SysLoginLogMapper sysLoginLogMapper;

    @Async("loginLogExecutor")
    public void recordSuccess(String username, Long userId, String loginIp, String userAgent, String sessionId) {
        save(buildLog(username, userId, loginIp, userAgent, 1, null, sessionId));
    }

    @Async("loginLogExecutor")
    public void recordFailure(String username, Long userId, String loginIp, String userAgent, String failReason) {
        save(buildLog(username, userId, loginIp, userAgent, 0, failReason, null));
    }

    private void save(SysLoginLog logEntry) {
        try {
            sysLoginLogMapper.insert(logEntry);
        } catch (Exception e) {
            log.warn("写入登录日志失败: username={}", logEntry.getUsername(), e);
        }
    }

    private SysLoginLog buildLog(String username, Long userId, String loginIp, String userAgent,
                                 Integer loginResult, String failReason, String sessionId) {
        SysLoginLog entry = new SysLoginLog();
        entry.setUsername(username);
        entry.setUserId(userId);
        entry.setLoginTime(LocalDateTime.now());
        entry.setLoginIp(loginIp);
        entry.setUserAgent(truncate(userAgent, 512));
        entry.setLoginResult(loginResult);
        entry.setFailReason(truncate(failReason, 128));
        entry.setSessionId(sessionId);
        return entry;
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
