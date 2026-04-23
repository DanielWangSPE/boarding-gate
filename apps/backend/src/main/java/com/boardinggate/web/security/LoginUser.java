package com.boardinggate.web.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 当前登录用户视图。仅包含过滤器从 Access Token 里解析出的最小信息，
 * 不与数据库实体耦合；Controller 如需更多字段请基于 userId 自行查询。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser {

    private Long userId;

    private String username;

    private String sessionId;

    /** Access Token 的 jti，便于审计 / 追踪单次请求。 */
    private String jti;
}
