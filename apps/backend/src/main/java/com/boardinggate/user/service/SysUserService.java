package com.boardinggate.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.boardinggate.user.entity.SysUser;
import com.boardinggate.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SysUserService {

    private final SysUserMapper sysUserMapper;

    public SysUser findByUsername(String username) {
        if (username == null || username.isEmpty()) {
            return null;
        }
        LambdaQueryWrapper<SysUser> qw = new LambdaQueryWrapper<>();
        qw.eq(SysUser::getUsername, username);
        qw.last("LIMIT 1");
        return sysUserMapper.selectOne(qw);
    }

    /** 记录最近一次登录成功的 IP 与时间，用于用户最近活跃信息展示。 */
    public void markLoginSuccess(Long userId, String loginIp) {
        LambdaUpdateWrapper<SysUser> uw = new LambdaUpdateWrapper<>();
        uw.eq(SysUser::getId, userId)
          .set(SysUser::getLastLoginTime, LocalDateTime.now())
          .set(SysUser::getLastLoginIp, loginIp);
        sysUserMapper.update(null, uw);
    }
}
